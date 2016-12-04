package com.jautumn.parser.impl;

import static com.jautumn.utils.ReflectionUtils.getStringFromPrivateStaticField;
import static com.jautumn.utils.TestUtils.getFileNameWithPackage;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URL;

import javax.xml.xpath.XPathFactory;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.jautumn.parser.core.api.DownloadLinkParser;
import com.jautumn.utils.RequestUtils;

@RunWith(PowerMockRunner.class)
@PrepareForTest(RequestUtils.class)
public class DepositFilesDownloadLinkTypeParserTest {
    private static final String FID = "4fb13032c8fcf01556d293f4567c2770";
    private static final String START_URL = "https://depositfiles.com/files/i6g7xxoct";
    private static final String DOWNLOAD_URL = "http://fileshare1300.depositfiles.com/auth-14801721070dec2d4b56595b5deb4ce1-95.68.132.224-49009548-165180612-guest/FS130-2/learning-java-by-building-android-games.zip";

    private static final String DEPOSITE_FILES_DOWNLOAD_PAGE = getFileNameWithPackage("DepositFilesDownloadPage.html", DepositFilesDownloadLinkParser.class);
    private static final String GET_FILE_PAGE = getFileNameWithPackage("GetFile.html", DepositFilesDownloadLinkParser.class);

    private String getFileUrl;
    private String formBody;

    private String fidScriptTextPath;
    private String scriptTextPath;

    private XPathFactory xPathFactory;
    private WebClient clientMock;

    @Before
    public void setUp() throws Exception {
        setUpWebConstants();
        setUpXPathExpressions();
        xPathFactory = XPathFactory.newInstance();
        setUpClientMock();
    }

    private void setUpWebConstants() throws Exception {
        getFileUrl = getStringFromPrivateStaticField(DepositFilesDownloadLinkParser.class, "GET_FILE_URL");
        formBody = getStringFromPrivateStaticField(DepositFilesDownloadLinkParser.class, "FORM_BODY");
    }

    private void setUpXPathExpressions() throws Exception {
        fidScriptTextPath = getStringFromPrivateStaticField(DepositFilesDownloadLinkParser.class, "FID_SCRIPT_TEXT_PATH");
        scriptTextPath = getStringFromPrivateStaticField(DepositFilesDownloadLinkParser.class, "SCRIPT_TEXT_PATH");
    }

    private void setUpClientMock() throws Exception {
        PowerMockito.mockStatic(RequestUtils.class);

        WebRequest postRequest = new WebRequest(new URL(START_URL), HttpMethod.POST);
        postRequest.setRequestBody(formBody);
        when(RequestUtils.buildPost(START_URL, formBody)).thenReturn(postRequest);

        WebRequest getRequest = new WebRequest(new URL(getFileUrl + FID), HttpMethod.GET);
        when(RequestUtils.buildGet(getFileUrl + FID)).thenReturn(getRequest);

        clientMock = mock(WebClient.class);

        HtmlPage htmlPageMock = mock(HtmlPage.class);
        when(clientMock.getPage(postRequest)).thenReturn(htmlPageMock);
        when(clientMock.getPage(getRequest)).thenReturn(htmlPageMock);

        Node fidScriptNodeMock = mock(Node.class);

        when(fidScriptNodeMock.getNodeValue()).thenReturn(evalXPathExpression(fidScriptTextPath, DEPOSITE_FILES_DOWNLOAD_PAGE));
        when(htmlPageMock.getFirstByXPath(fidScriptTextPath)).thenReturn(fidScriptNodeMock);

        Node downloadUrlScriptNodeMock = mock(Node.class);
        when(downloadUrlScriptNodeMock.getNodeValue()).thenReturn(evalXPathExpression(scriptTextPath, GET_FILE_PAGE));
        when(htmlPageMock.getFirstByXPath(scriptTextPath)).thenReturn(downloadUrlScriptNodeMock);
    }

    private String evalXPathExpression(String xPathEx, String fileName) throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(fileName).getFile());
        TagNode tagNode = new HtmlCleaner().clean(file);

        Document dom = new DomSerializer(new CleanerProperties()).createDOM(tagNode);
        return xPathFactory.newXPath().evaluate(xPathEx, dom);
    }

    @Test
    public void testSuccessGetDownloadURL() throws Exception {
        DownloadLinkParser downloadLinkParser = new DepositFilesDownloadLinkParser(clientMock);
        String downloadURL = downloadLinkParser.getDownloadURL(START_URL);
        Assert.assertEquals(DOWNLOAD_URL, downloadURL);
    }

    @Test
    public void testFailureGetDownloadURL() throws Exception {
        //TODO
    }

}