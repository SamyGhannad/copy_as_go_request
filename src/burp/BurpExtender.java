package burp;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class BurpExtender implements IBurpExtender, ClipboardOwner, IContextMenuFactory {
  private final static String EXT_NAME = "Copy as Go request";
  private IExtensionHelpers helpers;
  private IBurpExtenderCallbacks callbacks;


  @Override
  public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks) {
    this.callbacks = callbacks;
    this.helpers = callbacks.getHelpers();
    callbacks.setExtensionName(EXT_NAME);
    callbacks.registerContextMenuFactory(this);
  }


  @Override
  public void lostOwnership(Clipboard clipboard, Transferable transferable) {

  }

  @Override
  public List<JMenuItem> createMenuItems(IContextMenuInvocation invocation) {
    final IHttpRequestResponse[] requestResponsePairs = invocation.getSelectedMessages();
    if (requestResponsePairs == null || requestResponsePairs.length == 0) return null;
    JMenuItem contextMenu1 = new JMenuItem(EXT_NAME);
    JMenuItem contextMenu2 = new JMenuItem(String.format("%s with base64 encoded body", EXT_NAME));
    contextMenu1.addActionListener(e -> buildRequest(requestResponsePairs[0], false));
    contextMenu2.addActionListener(e -> buildRequest(requestResponsePairs[0], true));
    return Arrays.asList(contextMenu1, contextMenu2);
  }

  private void buildRequest(IHttpRequestResponse requestResponsePair, boolean base64Encode) {
    StringBuilder goRequest = new StringBuilder();
    //Analyze the request and get information
    IRequestInfo requestInfo = helpers.analyzeRequest(requestResponsePair);
    // The user might select "Copy as Go request with base64 encoded body", but if the body
    // is actually empty, there's nothing to base64 encode, therefore we shouldn't be importing
    // encoding/base64`
    if (requestInfo.getBodyOffset() >= requestResponsePair.getRequest().length - 2) {
      base64Encode = false;
    }
    goRequest.append(Resources.GetRequestPrefix(base64Encode));
    goRequest.append(processHeaders(requestInfo.getHeaders()));
    goRequest.append(processBody(requestResponsePair.getRequest(), requestInfo, base64Encode));
    goRequest.append(createGoFunctionCall(requestInfo.getUrl().toString(), requestInfo.getMethod()));
    goRequest.append(Resources.RequestSuffix);
    Toolkit
        .getDefaultToolkit()
        .getSystemClipboard()
        .setContents(new StringSelection(goRequest.toString()), this);
  }

  private String processHeaders(List<String> headers) {
    // Remove the request type and URI, we need the rest of headers
    headers.remove(0);
    var goHeaders = new StringBuilder();
    goHeaders.append(String.format("%s%n", "  headers := map[string]string{"));
    for (var header : headers) {
      var splitHeader = header.split(":", 2);
      var key = splitHeader[0];
      var value = escape(splitHeader[1].strip());
      //We don't want to set Accept-Encoding in Go
      //If we do, then we have to manually handle decompression of gzip responses
      // but if don't set it, the default transport will automatically take care of that
      if (key.toLowerCase().equals("accept-encoding")) {
        continue;
      }
      goHeaders.append(String.format("    \"%s\": \"%s\",%n", key, value));
    }
    goHeaders.append(String.format("%s%n", "}"));
    return goHeaders.toString();
  }

  private String processBody(byte[] requestBytes, IRequestInfo requestInfo, boolean base64Encode) {
    var data = new StringBuilder();
    if (requestInfo.getBodyOffset() >= requestBytes.length - 2){
      data.append("  var data = []byte(nil)");
      return  String.format("%s%n", data);
    }

    var body = Arrays.copyOfRange(requestBytes, requestInfo.getBodyOffset(), requestBytes.length);

    if (base64Encode){
      data.append(String.format("  data, err := base64.StdEncoding.DecodeString(\"%s\")%n",
          new String(Base64.getEncoder().encode(body))));
      callbacks.printOutput(new String(Base64.getEncoder().encode(body)));
      data.append(String.format("%s%n","  if err != nil {"));
      data.append(String.format("%s%n", "    log.Fatal(\"error:\", err)"));
      data.append(String.format("%s%n", "}"));
    }else {
      data.append(String.format("  var data = []byte(\"%s\")",escape(new String(body))));
    }
    return String.format("%s%n", data);
  }

  private String createGoFunctionCall(String url, String httpMethod) {
    return String.format("  httpRequest(\"%s\", \"%s\", data, headers)%n%n", url, httpMethod);
  }

  private String escape(String input) {
    return input.replace("\\", "\\\\").replace("\"", "\\\"");
  }

}