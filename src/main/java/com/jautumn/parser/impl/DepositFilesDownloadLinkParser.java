package com.jautumn.parser.impl;

import static com.jautumn.utils.RequestUtils.buildGet;
import static com.jautumn.utils.RequestUtils.buildPost;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Node;

import com.gargoylesoftware.htmlunit.ProxyConfig;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.jautumn.parser.core.api.DownloadLinkParser;
import com.jautumn.parser.core.exceptions.BadDownloadServiceURLException;
import com.jautumn.parser.core.exceptions.ConnectionLimitException;
import com.jautumn.utils.TimeWatch;

public class DepositFilesDownloadLinkParser implements DownloadLinkParser {
    private static Logger logger = Logger.getLogger(DepositFilesDownloadLinkParser.class.getName());
    private static final TimeWatch timeWatch = new TimeWatch("Download service url processing time left:");

    private static final String CAN_NOT_DOWNLOAD_PAGE_MSG = "Can't com.jautumn.download page by url: %s";

    private static final String PART_OF_DOWNLOAD_URL_HOST = "fileshare";
    private static final String GET_FILE_URL = "https://depositfiles.com/get_file.php?fd2=";
    private static final String FORM_BODY = "gateway_result:1";

    private static final int CONNECTIONS_MAX_ATTEMPT = 100;
    private static final int CONNECTION_DELAY = 5000;

    private static final Pattern LINK_PATTERN = Pattern.compile("'.+'");

    private static final String FID_SCRIPT_TEXT_PATH = "//script[contains(text(), 'var fid')]/text()";
    private static final String SCRIPT_TEXT_PATH = "//script/text()";
    private static final String IP_LIMIT_MSG_PATH = "//div[@class = 'ipbg']/strong/text()";

    private WebClient client;
    private boolean proxyMode;

    public DepositFilesDownloadLinkParser(WebClient client, String proxyHost, int proxyPort) {
        ProxyConfig proxyConfig = new ProxyConfig(proxyHost, proxyPort);
        client.getOptions().setProxyConfig(proxyConfig);
        proxyMode = true;
        this.client = client;
    }

    public DepositFilesDownloadLinkParser(WebClient client) {
        this.client = client;
    }

    public static void main(String[] args) throws InterruptedException, ConnectionLimitException, BadDownloadServiceURLException, IOException {
        WebClient client = new WebClient();
        client.getOptions().setJavaScriptEnabled(false);
        client.getOptions().setCssEnabled(false);
        System.out.println(new DepositFilesDownloadLinkParser(client, "5.135.164.181", 3128).getDownloadURL("https://depositfiles.com/files/i6g7xxoct"));
    }

    @Override
    public String getDownloadURL
            (String startURL)
            throws IOException, InterruptedException, BadDownloadServiceURLException, ConnectionLimitException {

        logger.info("start processing");
        logger.info("getting file id");

        timeWatch.start();
        String fid = getURLFromPage(buildPost(startURL, FORM_BODY), FID_SCRIPT_TEXT_PATH);
        logger.info("got fid: " + fid);
        String getFileURL = GET_FILE_URL + fid;

        logger.info("getting file share id");
        String downloadURL;
        while (!(downloadURL = getURLFromPage(buildGet(getFileURL), SCRIPT_TEXT_PATH)).contains(PART_OF_DOWNLOAD_URL_HOST)) {
            logger.info(timeWatch.getTimeLeft());
            Thread.sleep(CONNECTION_DELAY);
        }
        timeWatch.stop();
        return downloadURL;
    }

    private String getURLFromPage
            (WebRequest request, String xPath)
            throws IOException, InterruptedException, ConnectionLimitException, BadDownloadServiceURLException {

        HtmlPage downloadPage = null;
        int i = 0;
        while (i < CONNECTIONS_MAX_ATTEMPT) {
            try {
                logger.info((i + 1) + " attempt");
                logger.info(timeWatch.getTimeLeft());
                downloadPage = client.getPage(request);
                break;
            } catch (Exception e) {
                if (i == CONNECTIONS_MAX_ATTEMPT - 1) {
                    if (proxyMode) {
                        client.getOptions().setProxyConfig(getNewProxyConfig());
                    }
                    return getURLFromPage(request, xPath);
                }
            }
            i++;
        }

        if (downloadPage == null) {
            throw new BadDownloadServiceURLException(CAN_NOT_DOWNLOAD_PAGE_MSG, request.getUrl().toString());
        }
        Node node = downloadPage.getFirstByXPath(xPath);

        if (node == null) {
            Node messageNode = downloadPage.getFirstByXPath(IP_LIMIT_MSG_PATH);
            if (messageNode != null) {
                throw new ConnectionLimitException(StringUtils.normalizeSpace(messageNode.getNodeValue()));
            } else {
                throw new BadDownloadServiceURLException(request.getUrl().toString());
            }
        }

        Matcher matcher = LINK_PATTERN.matcher(node.getNodeValue());
        matcher.find();
        return StringUtils.strip(matcher.group(), "'");
    }

    private ProxyConfig getNewProxyConfig() {
        logger.info("set new proxy config");
        return new ProxyConfig("137.135.166.225", 8124);
    }
}
