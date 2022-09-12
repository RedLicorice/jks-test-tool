# jks-test-tool
 A tool for testing HTTPS connectivity using JKS truststores.
 
 ## Why?
 
 How many times you updated a truststore only to find out the certificate you picked was the wrong one?
 How many times did you have to do it all from scratch?
 
 This tool addresses that, allowing you to test connectivity to a given host by means of a HTTPS Get request, in which certificates are validated against a given Java Trust Store, or the default one.
 
 ## How? 
 
 Usage is simple: the provided jar is bundled with all needed dependencies, just open a command prompt and use the command: 

        java -jar jks_test_tool-1.0.jar [-K <path_to_your_truststore.jks> -P <truststore_password>] -U <request_url>

 Available options can be checked by means of the "--help" switch, these are:

        -K/--keystore specifies path to the JKS keystore to use
        -P/--password the JKS keystore password.
                WARNING: Must be present if keystore is specified.
        -U/--url the URL to perform a GET query against
        -O/--output Print request output to STDOUT
 
 ## Requirements
 The tool was built using Zulu OpenJDK v17, but it should run with any version of JRE 11+.
 
 ## Support 
 The software is provided as-is, use is granted to the end-user according to the license terms (LICENSE).

 The author can not in any way be held responsible for any possible harm or damage deriving from the use of this product.

 Found a bug? -> Open an issue!

 Want to submit an improvement? -> Clone, patch and fill in a PR!
 
 
