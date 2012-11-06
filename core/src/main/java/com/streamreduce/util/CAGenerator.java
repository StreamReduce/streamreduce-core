/*
 * Copyright 2012 Nodeable Inc
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.streamreduce.util;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;
import org.bouncycastle.x509.extension.SubjectKeyIdentifierStructure;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

/**
 * Generates new SSL certificates for use with the agent.
 */
public final class CAGenerator {

    public static void main(String[] args) throws Exception {

        KeyStore store = KeyStore.getInstance("JKS");
//        store.load(CAGenerator.class.getResourceAsStream("/mmc-keystore.jks"), "ion-mmc".toCharArray());
        store.load(null);

        KeyPair keypair = generateKeyPair();

        X509Certificate cert = generateCACert(keypair);

        char[] password = "nodeable-agent".toCharArray();
        store.setKeyEntry("nodeable", keypair.getPrivate(), password, new Certificate[]{cert});
        store.store(new FileOutputStream("nodeable-keystore.jks"), password);
        byte[] certBytes = getCertificateAsBytes(cert);
        FileOutputStream output = new FileOutputStream("nodeable.crt");
        IOUtils.copy(new ByteArrayInputStream(certBytes), output);
        output.close();
    }

    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024);
        return keyGen.generateKeyPair();
    }

    public static X509Certificate generateCACert(KeyPair keyPair) throws Exception {
        Date startDate = new Date(System.currentTimeMillis());                // time from which certificate is valid
        Calendar expiry = Calendar.getInstance();
        expiry.add(Calendar.DAY_OF_YEAR, 1000 * 365);
        Date expiryDate = expiry.getTime();               // time after which certificate is not valid
        BigInteger serialNumber = new BigInteger(Long.toString(System.currentTimeMillis()));       // serial number for certificate

        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
        X500Principal dnName = new X500Principal("CN=Nodeable Client");

        certGen.setSerialNumber(serialNumber);
        certGen.setIssuerDN(dnName);
        certGen.setNotBefore(startDate);
        certGen.setNotAfter(expiryDate);
        certGen.setSubjectDN(dnName);
        certGen.setPublicKey(keyPair.getPublic());
        certGen.setSignatureAlgorithm("MD5withRSA");

        certGen.addExtension(X509Extensions.AuthorityKeyIdentifier, false,
                new AuthorityKeyIdentifierStructure(keyPair.getPublic()));
        certGen.addExtension(X509Extensions.SubjectKeyIdentifier, false,
                new SubjectKeyIdentifierStructure(keyPair.getPublic()));

        return certGen.generate(keyPair.getPrivate());   // note: private key of CA
    }

    public static byte[] getCertificateAsBytes(final X509Certificate cert) throws IOException {
        StringWriter writer = new StringWriter();
        PEMWriter pemW = new PEMWriter(writer);
        pemW.writeObject(cert);
        pemW.close();
        return writer.getBuffer().toString().getBytes();
    }

}
