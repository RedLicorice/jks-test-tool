package com.github.redlicorice;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustSelfSignedStrategy;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.ssl.SSLContexts;


public class App 
{
    private final static KeyStore loadKeystore(String filename, String password){
        try{
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(filename), password.toCharArray());
            return ks;
        }
        catch(java.io.FileNotFoundException e){
            System.err.println(String.format("Keystore file not found at %s", filename));
        }
        catch(java.security.KeyStoreException e){
            System.err.println("Caugt KeyStoreException!");
            e.printStackTrace();
        }
        catch(java.io.IOException e){
            System.err.println("Caugt IOException!");
            e.printStackTrace();
        }
        catch(java.security.NoSuchAlgorithmException e){
            System.err.println("Caugt NoSuchAlgorithmException!");
            e.printStackTrace();
        }
        catch(CertificateException e){
            System.err.println("Caugt CertificateException!");
            e.printStackTrace();
        }
        return null;
    }

    public final static void get(String keystore, String password, String request_uri, Boolean printResponse) throws Exception {
        KeyStore ks = loadKeystore(keystore, password);
        // Trust standard CA and those trusted by our custom strategy
        final SSLContext sslcontext;
        if(keystore.isEmpty()){
            sslcontext = SSLContexts.createSystemDefault();
            System.out.println("WARNING: Using System Default SSL Context!");
        } else {
            sslcontext = SSLContexts.custom()
            .loadTrustMaterial(ks, new TrustSelfSignedStrategy())
            .build();
        }

        // Allow TLSv1.2 protocol only
        final SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
            .setSslContext(sslcontext)
            .setTlsVersions(TLS.V_1_2)
            .build();
        final HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
            .setSSLSocketFactory(sslSocketFactory)
            .build();
        
        try (CloseableHttpClient httpclient = HttpClients.custom().setConnectionManager(cm).build()) {

            final HttpGet httpget = new HttpGet(request_uri);

            System.out.println("Executing request " + httpget.getMethod() + " " + httpget.getUri());

            final HttpClientContext clientContext = HttpClientContext.create();
            //             try (CloseableHttpResponse response = httpclient.execute(httpget, clientContext)) {
            try (CloseableHttpResponse response = httpclient.execute(httpget, clientContext)) {
                System.out.println("----------------------------------------");
                System.out.println("Success! Response code: " + response.getCode() + " Reason: " + response.getReasonPhrase());
                if(printResponse){
                    System.out.println(EntityUtils.toString(response.getEntity()));

                    final SSLSession sslSession = clientContext.getSSLSession();
                    if (sslSession != null) {
                        System.out.println("SSL protocol " + sslSession.getProtocol());
                        System.out.println("SSL cipher suite " + sslSession.getCipherSuite());
                    }
                }
            }
            catch(SSLHandshakeException e){
                System.err.println("/!\\ SSLHandshakeException caught. Test failed!");
            }
        }
    }

    public static void main( String[] args )
    {
        CommandLine commandLine;
        
        Options options = new Options();
        options.addOption(new Option("K", "keystore", true, "Java KeyStore to use, in JKS format"));
        options.addOption(new Option("P", "password", true, "The password for decrypting the provided Java KeyStore"));
        options.addOption(new Option("U", "url", true, "The URL to fetch"));
        options.addOption(new Option("H", "help", false, "Show help message"));
        options.addOption(new Option("O", "output", false, "Print request output to STDOUT"));
        
        CommandLineParser parser = new DefaultParser();
        
        try
        {
            commandLine = parser.parse(options, args);
            if (commandLine.hasOption("H")){
                System.out.println("Usage:");
                System.out.println(String.format("%s [-K <<jks truststore>> -P <<jks password>>] -U <<url>>\n", args[0]));
                System.out.println("\t-K/--keystore specifies path to the JKS keystore to use");
                System.out.println("\t-P/--password the JKS keystore password. \n\t\tWARNING: Must be present if keystore is specified.");
                System.out.println("\t-U/--url the URL to perform a GET query against");
                System.out.println("\t-O/--output Print request output to STDOUT");
                System.exit(1);
            }

            String keystore = new String();
            String password = new String();

            if (!commandLine.hasOption("U")){
                System.err.println("Must specify an URL.");
                System.exit(1);
            }
            String url = commandLine.getOptionValue("U");

            if (commandLine.hasOption("K"))
            {
                if (!commandLine.hasOption("P")){
                    System.err.println("Must specify a password when providing a keystore.");
                    System.exit(1);
                }
                keystore = commandLine.getOptionValue("K");
                password = commandLine.getOptionValue("P");
                System.out.println(String.format("Info: Using %s as SSL Context truststore.", keystore));
            }

            try{
                get(keystore, password, url, commandLine.hasOption("O"));
            }
            catch(Exception e){
                System.err.println(String.format("Error performing request to %s", url));
                e.printStackTrace();
            }

        }
        catch (ParseException exception)
        {
            System.out.print("Parse error: ");
            System.out.println(exception.getMessage());
        }
    }
}
