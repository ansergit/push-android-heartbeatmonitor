/*
 * Copyright (C) 2014 Pivotal Software, Inc. All rights reserved.
 */
package io.pivotal.android.push.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import io.pivotal.android.push.PushParameters;
import io.pivotal.android.push.prefs.Pivotal;
import io.pivotal.android.push.receiver.CustomSslProvider;

import static java.util.Collections.list;

public class ApiRequestImpl {

    public static final String CUSTOM_SSL_PROVIDER_META_DATA = "io.pivotal.android.push.CustomSslProvider";

    protected NetworkWrapper networkWrapper;
    protected Context context;

    protected ApiRequestImpl(Context context, NetworkWrapper networkWrapper) {
        verifyArguments(context, networkWrapper);
        saveArguments(context, networkWrapper);
    }

    public static String getBasicAuthorizationValue(PushParameters parameters) {
        final String stringToEncode = parameters.getPlatformUuid() + ":" + parameters.getPlatformSecret();
        return "Basic  " + Base64.encodeToString(stringToEncode.getBytes(), Base64.DEFAULT | Base64.NO_WRAP);
    }

    private void verifyArguments(Context context, NetworkWrapper networkWrapper) {
        if (networkWrapper == null) {
            throw new IllegalArgumentException("networkWrapper may not be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("context may not be null");
        }
    }

    private void saveArguments(Context context, NetworkWrapper networkWrapper) {
        this.networkWrapper = networkWrapper;
        this.context = context;
    }

    protected HttpURLConnection getHttpURLConnection(URL url, PushParameters parameters) throws IOException, IllegalAccessException, GeneralSecurityException, InstantiationException {
        final HttpURLConnection urlConnection = networkWrapper.getHttpURLConnection(url);
        urlConnection.setReadTimeout(60000);
        urlConnection.setConnectTimeout(60000);
        urlConnection.setChunkedStreamingMode(0);
        addCustomRequestHeaders(parameters, urlConnection);
        setupTrust(parameters, urlConnection);
        return urlConnection;
    }

    protected void writeOutput(String requestBodyData, OutputStream outputStream) throws IOException {
        final byte[] bytes = requestBodyData.getBytes();
        for (byte b : bytes) {
            outputStream.write(b);
        }
        outputStream.close();
    }

    protected String readInput(InputStream inputStream) throws IOException {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[256];

        while (true) {
            final int numberBytesRead = inputStream.read(buffer);
            if (numberBytesRead < 0) {
                break;
            }
            byteArrayOutputStream.write(buffer, 0, numberBytesRead);
        }

        final String str = new String(byteArrayOutputStream.toByteArray());
        return str;
    }

    protected boolean isFailureStatusCode(int statusCode) {
        return (statusCode < 200 || statusCode >= 300);
    }

    protected boolean isFatalStatusCode(int statusCode) {
        return (statusCode >= 400 && statusCode < 500);
    }

    protected void addCustomRequestHeaders(PushParameters parameters, HttpURLConnection urlConnection) {
        final Map<String, String> requestHeaders = parameters.getRequestHeaders();
        if (requestHeaders != null && !requestHeaders.isEmpty()) {
            for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
                urlConnection.addRequestProperty(entry.getKey(), entry.getValue());
            }
        }
    }

    protected void setupTrust(PushParameters parameters, HttpURLConnection urlConnection) throws GeneralSecurityException, IOException, IllegalAccessException, InstantiationException {

        if (urlConnection instanceof HttpsURLConnection) {

            final HttpsURLConnection httpsURLConnection = (HttpsURLConnection) urlConnection;

            if (parameters.getSslCertValidationMode() == Pivotal.SslCertValidationMode.TRUST_ALL) {
                trustAllSslCertificates(httpsURLConnection);

            } else if (parameters.getSslCertValidationMode() == Pivotal.SslCertValidationMode.PINNED && parameters.getPinnedSslCertificateNames() != null && parameters.getPinnedSslCertificateNames().size() > 0) {
                trustPinnedSslCertificates(context, parameters, httpsURLConnection);

            } else if (parameters.getSslCertValidationMode() == Pivotal.SslCertValidationMode.CALLBACK) {

                Logger.w("Note: Using a custom callback for SSL authentication in PCF Push.");

                final Class<? extends CustomSslProvider> customSslProviderClass = getCustomSslProviderClass(context);
                final CustomSslProvider customSslProvider = customSslProviderClass.newInstance();
                final SSLSocketFactory socketFactory = customSslProvider.getSSLSocketFactory();
                final HostnameVerifier hostnameVerifier = customSslProvider.getHostnameVerifier();

                if (socketFactory != null) {
                    httpsURLConnection.setSSLSocketFactory(socketFactory);
                }
                if (hostnameVerifier != null)  {
                    httpsURLConnection.setHostnameVerifier(hostnameVerifier);
                }

            } else {
                Logger.w("Note: Using system default SSL authentication in PCF Push.");
            }
        }
    }

    private void trustAllSslCertificates(HttpsURLConnection urlConnection) throws NoSuchAlgorithmException, KeyManagementException {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }};

        // Ignore differences between given hostname and certificate hostname
        HostnameVerifier hv = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) { return true; }
        };

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, trustAllCerts, null);

        urlConnection.setSSLSocketFactory(context.getSocketFactory());
        urlConnection.setHostnameVerifier(hv);

        Logger.w("Note: We trust all SSL certifications in PCF Push.");
    }

    private void trustPinnedSslCertificates(Context context, PushParameters parameters, HttpsURLConnection urlConnection) throws GeneralSecurityException, IOException {

        // Load CAs from an InputStream
        final KeyStore keyStore = getKeyStore(context, parameters);

        // Create a trust manager that validates a pinned certificate
        TrustManager[] trustPinnedManager = new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        if (chain == null) {
                            throw new IllegalArgumentException("checkServerTrusted: X509Certificate array is null");
                        }

                        if (!(chain.length > 0)) {
                            throw new IllegalArgumentException("checkServerTrusted: X509Certificate is empty");
                        }

                        if (!(authType != null && authType.contains("RSA"))) {
                            throw new CertificateException("checkServerTrusted: AuthType is not RSA.  AuthType: " + authType);
                        }

                        boolean foundMatchingCertificate = false;

                        // Remote certificate
                        RSAPublicKey remotePubKey = (RSAPublicKey) chain[0].getPublicKey();
                        String remoteEncodedPubKey = new BigInteger(1, remotePubKey.getEncoded()).toString(16);

                        String localEncodedPubKey;
                        // Perform custom SSL/TLS checks
                        try {
                            TrustManagerFactory trustMgrFactory = TrustManagerFactory.getInstance("X509");
                            trustMgrFactory.init(keyStore);

                            for (TrustManager trustManager : trustMgrFactory.getTrustManagers()) {
                                ((X509TrustManager) trustManager).checkServerTrusted(chain, authType);
                            }

                            for (String alias : list(keyStore.aliases())) {
                                // Local certificate
                                RSAPublicKey localPubKey = (RSAPublicKey) keyStore.getCertificate(alias).getPublicKey();
                                localEncodedPubKey = new BigInteger(1, localPubKey.getEncoded()).toString(16);

                                foundMatchingCertificate = localEncodedPubKey.equalsIgnoreCase(remoteEncodedPubKey);

                                if (foundMatchingCertificate)
                                    break;
                            }

                        } catch (Exception error) {
                            throw new CertificateException(error);
                        }

                        // Pin it!
                        if (!foundMatchingCertificate) {
                            throw new CertificateException("The server's certificate has not been authenticated.");
                        }
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustPinnedManager, null);

        urlConnection.setSSLSocketFactory(sslContext.getSocketFactory());

        Logger.w("Note: Authenticating certificate in PCF Push.");
    }

    private KeyStore getKeyStore(Context context, PushParameters parameters) throws GeneralSecurityException, IOException {

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        String keyStoreType = KeyStore.getDefaultType();
        final KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        keyStore.load(null, null);

        List<String> pinnedCertificateNames = parameters.getPinnedSslCertificateNames();
        for (int i = 0; i < pinnedCertificateNames.size(); i += 1) {

            String pinnedCertificateName = pinnedCertificateNames.get(i);
            InputStream caInput = null;
            Certificate ca = null;

            try {
                if (pinnedCertificateName != null) {
                    caInput = context.getAssets().open(pinnedCertificateName);
                    ca = cf.generateCertificate(caInput);
                    Logger.i("Note: We are pinning certificate '" + pinnedCertificateName + "'.");
                }

            } catch (IOException e1) {
                Logger.w("WARNING: could not open certificate file '" + pinnedCertificateName + "': " + e1);

            } catch (CertificateException e2) {
                Logger.w("WARNING: could not read certificate file '" + pinnedCertificateName + "': " + e2);

            } finally {
                if (caInput != null) {
                    caInput.close();
                }
            }

            // Create a KeyStore containing our trusted CAs
            if (ca != null) {
                keyStore.setCertificateEntry(String.valueOf(i), ca);
            }
        }

        return keyStore;
    }

    public static Class<? extends CustomSslProvider> getCustomSslProviderClass(final Context context) {
        try {
            final Class<? extends CustomSslProvider> klass = ApiRequestImpl.findProviderClassName(context);
            if (klass != null) return klass;
        } catch (Exception e) {
            Logger.ex(e);
        }

        return CustomSslProvider.class;
    }

    private static Class<? extends CustomSslProvider> findProviderClassName(final Context context) throws PackageManager.NameNotFoundException, ClassNotFoundException {
        final PackageManager manager = context.getPackageManager();
        final ApplicationInfo applicationInfo = manager.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
        if (applicationInfo != null) {
            final Bundle metaData = applicationInfo.metaData;
            if (metaData != null) {
                if (metaData.containsKey(CUSTOM_SSL_PROVIDER_META_DATA)) {
                    final String klassName = metaData.getString(CUSTOM_SSL_PROVIDER_META_DATA);
                    if (klassName != null) {
                        final Class<?> klass = Class.forName(klassName);
                        if (klass != null && CustomSslProvider.class.isAssignableFrom(klass)) {
                            return (Class<? extends CustomSslProvider>) klass;
                        }
                    }
                }
            }
        }
        return null;
    }
}
