package burp;

public class Resources {
  public static String GetRequestPrefix(boolean base64Encode){
    // Should we import encoding/base64 or not?
    if (base64Encode) {
      return String.format("%s%n%s%n%s%n%s%n%s%n%s%n%s%n%s%n%s%n%s%n%s%n%s%n%s%n",
          "package main",
          "",
          "import (",
          "	\"bytes\"",
          "	\"encoding/base64\"",
          "	\"crypto/tls\"",
          "	\"fmt\"",
          "	\"log\"",
          "	\"io\"",
          "	\"net/http\"",
          ")",
          "",
          "func main() {",
          "");
    }
    return String.format("%s%n%s%n%s%n%s%n%s%n%s%n%s%n%s%n%s%n%s%n%s%n%s%n",
        "package main",
        "",
        "import (",
        "	\"bytes\"",
        "	\"crypto/tls\"",
        "	\"fmt\"",
        "	\"io\"",
        "	\"net/http\"",
        ")",
        "",
        "func main() {",
        "");
  }
  public static String RequestSuffix = String.format("%s%n%s%n%s%n%s%n%s%n%s%n%s%n%s%n%s%n%s%n%s%n%s%n%s%n%s%n%s%n%s%n%s%n%s%n%s%n%s%n%s%n%s%n%s%n%s%n%s%n%s%n","}",
      "",
      "func httpRequest(targetUrl string, method string, data []byte, headers map[string]string) *http.Response {",
      "",
      "	request, error := http.NewRequest(method, targetUrl, bytes.NewBuffer(data))",
      "	for k, v := range headers {",
      "		request.Header.Set(k, v)",
      "",
      "	}",
      "",
      "	customTransport := &http.Transport{",
      "		TLSClientConfig: &tls.Config{InsecureSkipVerify: true},",
      "	}",
      "	client := &http.Client{Transport: customTransport}",
      "	response, error := client.Do(request)",
      "	defer response.Body.Close()",
      "",
      "	if error != nil {",
      "		panic(error)",
      "	}",
      "",
      "	body, _ := io.ReadAll(response.Body)",
      "	fmt.Println(\"response Status:\", response.Status)",
      "	fmt.Println(\"response Body:\", string(body))",
      "	return response",
      "}",
      ""
      );
}
