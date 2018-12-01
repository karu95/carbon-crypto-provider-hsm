/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.crypto.provider.hsm;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.base.ServerConfigurationException;
import org.wso2.carbon.crypto.api.CertificateInfo;
import org.wso2.carbon.crypto.api.CryptoContext;
import org.wso2.carbon.crypto.api.CryptoException;
import org.wso2.carbon.crypto.api.HybridEncryptionInput;
import org.wso2.carbon.crypto.api.HybridEncryptionOutput;
import org.wso2.carbon.crypto.api.PrivateKeyInfo;
import org.wso2.carbon.crypto.provider.hsm.cryptoprovider.util.MechanismResolver;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

public class HSMBasedExternalCryptoProviderTest {

    private HSMBasedExternalCryptoProvider hsmBasedExternalCryptoProvider;
    private HSMBasedKeyResolver hsmBasedKeyResolver;
    private ArrayList<CryptoContext> sampleCryptoContexts = new ArrayList<>();
    private ArrayList<byte[]> sampleData = new ArrayList<>();
    private ArrayList<byte[]> sampleSignatures = new ArrayList<>();
    private ArrayList<byte[]> sampleEncryptedData = new ArrayList<>();
    private ArrayList<HybridEncryptionInput> sampleHybridEncryptionInputs = new ArrayList<>();
    private ArrayList<HybridEncryptionOutput> sampleHybridEncryptedData = new ArrayList<>();

    @BeforeClass
    public void setUpClass() throws ServerConfigurationException, CryptoException {

        hsmBasedKeyResolver = new HSMBasedKeyResolver(TestUtil.getServerConfigurationService());
        hsmBasedExternalCryptoProvider = new HSMBasedExternalCryptoProvider(TestUtil.getServerConfigurationService());
    }

    @Test(dataProvider = "sampleSignDataProvider")
    public void testSign(byte[] data, String algorithm, String javaSecurityAPIProvider, CryptoContext cryptoContext,
                         PrivateKeyInfo privateKeyInfo) {

        try {
            byte[] signature = hsmBasedExternalCryptoProvider
                    .sign(data, algorithm, javaSecurityAPIProvider, cryptoContext, privateKeyInfo);
            sampleSignatures.add(signature);
        } catch (CryptoException e) {
            if (!(algorithm != null && MechanismResolver.getSupportedMechanisms().containsKey(algorithm))) {
                Assert.assertEquals(e.getMessage(),
                        String.format("Requested algorithm '%s' is not valid/supported.", algorithm));
            }
        }
    }

    @DataProvider(name = "sampleSignDataProvider")
    public Object[][] getSignData() {

        sampleData.add("Sample 1 for testing".getBytes());
        sampleData.add(new byte[1000]);
        sampleData.add("".getBytes());
        sampleData.add(null);

        sampleCryptoContexts.add(CryptoContext.buildEmptyContext(-1234, "carbon.super"));
        sampleCryptoContexts.add((CryptoContext.buildEmptyContext(1, "wso2.lk")));

        return new Object[][]{
                {
                        sampleData.get(0), "SHA1withRSAandMGF1", null, sampleCryptoContexts.get(0),
                        hsmBasedKeyResolver.getPrivateKeyInfo(sampleCryptoContexts.get(0))

                },
                {
                        sampleData.get(1), "SHA384withRSAandMGF1", null, sampleCryptoContexts.get(1),
                        hsmBasedKeyResolver.getPrivateKeyInfo(sampleCryptoContexts.get(1))

                },
                {
                        sampleData.get(2), "SHA384withRSA", null, sampleCryptoContexts.get(0),
                        hsmBasedKeyResolver.getPrivateKeyInfo(sampleCryptoContexts.get(0))

                },
                {
                        sampleData.get(3), "MD5withRSA", null, sampleCryptoContexts.get(0),
                        hsmBasedKeyResolver.getPrivateKeyInfo(sampleCryptoContexts.get(0))
                },
                {
                        sampleData.get(0), "SHA132", null, sampleCryptoContexts.get(0),
                        hsmBasedKeyResolver.getPrivateKeyInfo(sampleCryptoContexts.get(0))

                }
        };
    }

    @Test(dataProvider = "sampleDecryptDataProvider", priority = 2)
    public void testDecrypt(byte[] ciphertext, String algorithm, String javaSecurityAPIProvider,
                            CryptoContext cryptoContext, PrivateKeyInfo privateKeyInfo, byte[] expectedDecryptData) {

        try {
            byte[] decryptedData = hsmBasedExternalCryptoProvider.decrypt(ciphertext, algorithm,
                    javaSecurityAPIProvider, cryptoContext, privateKeyInfo);
            Assert.assertEquals(decryptedData, expectedDecryptData);
        } catch (CryptoException e) {
            if (!(algorithm != null && MechanismResolver.getSupportedMechanisms().containsKey(algorithm))) {
                Assert.assertEquals(e.getMessage(),
                        String.format("Requested algorithm '%s' is not valid/supported.", algorithm));
            }
        }
    }

    @DataProvider(name = "sampleDecryptDataProvider")
    public Object[][] getDecryptSampleData() {

        return new Object[][]{
                {
                        sampleEncryptedData.get(0), "RSA/ECB/PKCS1Padding", null, sampleCryptoContexts.get(0),
                        hsmBasedKeyResolver.getPrivateKeyInfo(sampleCryptoContexts.get(0)), sampleData.get(0)
                },
                {
                        sampleEncryptedData.get(1), "RSA/ECB/OAEPwithSHA384andMGF1Padding", null, sampleCryptoContexts.get(1),
                        hsmBasedKeyResolver.getPrivateKeyInfo(sampleCryptoContexts.get(1)), sampleData.get(1)
                },
                {
                        sampleEncryptedData.get(2), "RSA/ECB/OAEPwithSHA1andMGF1Padding", null, sampleCryptoContexts.get(0),
                        hsmBasedKeyResolver.getPrivateKeyInfo(sampleCryptoContexts.get(0)), sampleData.get(2)
                },
                {
                        sampleEncryptedData.get(3), null, null, sampleCryptoContexts.get(0),
                        hsmBasedKeyResolver.getPrivateKeyInfo(sampleCryptoContexts.get(0)), sampleData.get(3)
                }
        };
    }

    @Test(dataProvider = "sampleEncryptDataProvider", priority = 1)
    public void testEncrypt(byte[] data, String algorithm, String javaSecurityAPIProvider,
                            CryptoContext cryptoContext, CertificateInfo certificateInfo) {

        try {
            byte[] encryptedData = hsmBasedExternalCryptoProvider
                    .encrypt(data, algorithm, javaSecurityAPIProvider, cryptoContext, certificateInfo);
            sampleEncryptedData.add(encryptedData);
        } catch (CryptoException e) {
            if (!(algorithm != null && MechanismResolver.getSupportedMechanisms().containsKey(algorithm))) {
                Assert.assertEquals(e.getMessage(),
                        String.format("Requested algorithm '%s' is not valid/supported.", algorithm));
            }
            sampleEncryptedData.add(null);
        }
    }

    @DataProvider(name = "sampleEncryptDataProvider")
    public Object[][] getEncryptSampleData() {

        return new Object[][]{
                {
                        sampleData.get(0), "RSA/ECB/PKCS1Padding", null, sampleCryptoContexts.get(0),
                        hsmBasedKeyResolver.getCertificateInfo(sampleCryptoContexts.get(0))
                },
                {
                        sampleData.get(1), "RSA/ECB/OAEPwithSHA384andMGF1Padding", null, sampleCryptoContexts.get(1),
                        hsmBasedKeyResolver.getCertificateInfo(sampleCryptoContexts.get(1))
                },
                {
                        sampleData.get(2), "RSA/ECB/OAEPwithSHA1andMGF1Padding", null, sampleCryptoContexts.get(0),
                        hsmBasedKeyResolver.getCertificateInfo(sampleCryptoContexts.get(0))
                },
                {
                        sampleData.get(3), null, null, sampleCryptoContexts.get(0),
                        hsmBasedKeyResolver.getCertificateInfo(sampleCryptoContexts.get(0))
                }
        };
    }

    @Test(dataProvider = "sampleVerifyDataProvider", priority = 2)
    public void testVerifySignature(byte[] data, byte[] signature, String algorithm, String javaSecurityAPIProvider,
                                    CryptoContext cryptoContext, CertificateInfo certificateInfo,
                                    boolean expectedResult) {

        try {
            boolean verification = hsmBasedExternalCryptoProvider.verifySignature(data, signature, algorithm,
                    javaSecurityAPIProvider, cryptoContext, certificateInfo);
            Assert.assertEquals(verification, expectedResult);
        } catch (CryptoException e) {
            if (!(algorithm != null && MechanismResolver.getSupportedMechanisms().containsKey(algorithm))) {
                Assert.assertEquals(e.getMessage(),
                        String.format("Requested algorithm '%s' is not valid/supported.", algorithm));
            }
        }
    }

    @DataProvider(name = "sampleVerifyDataProvider")
    public Object[][] getSampleVerifyData() {

        return new Object[][]{
                {
                        sampleData.get(0), sampleSignatures.get(0), "SHA1withRSAandMGF1", null, sampleCryptoContexts
                        .get(0), hsmBasedKeyResolver.getCertificateInfo(sampleCryptoContexts.get(0)), true

                },
                {
                        sampleData.get(1), sampleSignatures.get(1), "SHA384withRSAandMGF1", null, sampleCryptoContexts
                        .get(1), hsmBasedKeyResolver.getCertificateInfo(sampleCryptoContexts.get(1)), true

                },
                {
                        sampleData.get(2), sampleSignatures.get(0), "SHA384withRSA", null, sampleCryptoContexts.get(0),
                        hsmBasedKeyResolver.getCertificateInfo(sampleCryptoContexts.get(0)), false

                },
                {
                        sampleData.get(3), sampleSignatures.get(3), "MD5withRSA", null, sampleCryptoContexts.get(0),
                        hsmBasedKeyResolver.getCertificateInfo(sampleCryptoContexts.get(0)), true
                },
                {
                        sampleData.get(0), sampleSignatures.get(0), null, null, sampleCryptoContexts.get(0),
                        hsmBasedKeyResolver.getCertificateInfo(sampleCryptoContexts.get(0)), true
                }
        };
    }

    @Test(dataProvider = "sampleCertificateDataProvider", priority = 1)
    public void testGetCertificate(CryptoContext cryptoContext, CertificateInfo certificateInfo) {

        try {
            X509Certificate certificate = (X509Certificate) hsmBasedExternalCryptoProvider
                    .getCertificate(cryptoContext, certificateInfo);
            Assert.assertNotNull(certificate);
        } catch (CryptoException e) {
            if (cryptoContext == null) {
                Assert.assertEquals(e.getMessage(), "Tenant information is missing in the crypto context.");
            }
        }
    }

    @DataProvider(name = "sampleCertificateDataProvider")
    public Object[][] getSampleCertificateData() {

        return new Object[][]{
                {
                        sampleCryptoContexts.get(0), hsmBasedKeyResolver.getCertificateInfo(sampleCryptoContexts.get(0))
                },
                {
                        sampleCryptoContexts.get(1), hsmBasedKeyResolver.getCertificateInfo(sampleCryptoContexts.get(1))
                },
                {
                        sampleCryptoContexts.get(0), hsmBasedKeyResolver.getCertificateInfo(sampleCryptoContexts.get(0))
                }
        };
    }

    @Test(dataProvider = "samplePrivateKeyDataProvider", priority = 1)
    public void testGetPrivateKey(CryptoContext cryptoContext, PrivateKeyInfo privateKeyInfo) {

        try {
            PrivateKey retrievedKey = hsmBasedExternalCryptoProvider.getPrivateKey(cryptoContext, privateKeyInfo);
            Assert.assertNotNull(retrievedKey);
        } catch (CryptoException e) {
            if (cryptoContext == null) {
                Assert.assertEquals(e.getMessage(), "Tenant information is missing in the crypto context.");
            }
        }
    }

    @DataProvider(name = "samplePrivateKeyDataProvider")
    public Object[][] getSamplePrivateKeyData() {

        CryptoContext sampleContext1 = CryptoContext.buildEmptyContext(2, "def.com");
        CryptoContext sampleContext2 = CryptoContext.buildEmptyContext(3, "sample.com");

        return new Object[][]{
                {
                        sampleCryptoContexts.get(0), hsmBasedKeyResolver.getPrivateKeyInfo(sampleCryptoContexts.get(0))
                },
                {
                        sampleCryptoContexts.get(1), hsmBasedKeyResolver.getPrivateKeyInfo(sampleCryptoContexts.get(1))
                },
                {
                        sampleCryptoContexts.get(0), hsmBasedKeyResolver.getPrivateKeyInfo(sampleCryptoContexts.get(0))
                },
                {
                        sampleContext1, hsmBasedKeyResolver.getPrivateKeyInfo(sampleContext1)
                },
                {
                        sampleContext2, hsmBasedKeyResolver.getPrivateKeyInfo(sampleContext2)
                }
        };
    }

    @Test(dataProvider = "sampleHybridEncryptDataProvider", priority = 1)
    public void testHybridEncrypt(HybridEncryptionInput hybridEncryptionInput, String symmetricAlgorithm,
                                  String asymmetricAlgorithm, String javaSecurityProvider,
                                  CryptoContext cryptoContext, CertificateInfo certificateInfo) throws CryptoException {

        HybridEncryptionOutput hybridEncryptionOutput = hsmBasedExternalCryptoProvider.hybridEncrypt(
                hybridEncryptionInput, symmetricAlgorithm, asymmetricAlgorithm, javaSecurityProvider,
                cryptoContext, certificateInfo);
        sampleHybridEncryptedData.add(hybridEncryptionOutput);
    }

    @DataProvider(name = "sampleHybridEncryptDataProvider")
    public Object[][] getSampleHybridEncryptData() {

        sampleHybridEncryptionInputs.add(new HybridEncryptionInput(sampleData.get(0), new byte[20]));
        sampleHybridEncryptionInputs.add(new HybridEncryptionInput(sampleData.get(1)));
        sampleHybridEncryptionInputs.add(new HybridEncryptionInput(sampleData.get(2), new byte[10]));
        sampleHybridEncryptionInputs.add(new HybridEncryptionInput(sampleData.get(0)));

        return new Object[][]{
                {
                        sampleHybridEncryptionInputs.get(0), "AES_192/GCM/NoPadding", "RSA/ECB/OAEPwithSHA1andMGF1Padding", null,
                        sampleCryptoContexts.get(0), hsmBasedKeyResolver.getCertificateInfo(sampleCryptoContexts.get(0))
                },
                {
                        sampleHybridEncryptionInputs.get(1), "AES_256/CBC/PKCS5Padding", "RSA/ECB/PKCS1Padding", null,
                        sampleCryptoContexts.get(1), hsmBasedKeyResolver.getCertificateInfo(sampleCryptoContexts.get(1))
                },
                {
                        sampleHybridEncryptionInputs.get(2), "DES/CBC/PKCS5Padding", "RSA/ECB/OAEPwithMD5andMGF1Padding", null,
                        sampleCryptoContexts.get(0), hsmBasedKeyResolver.getCertificateInfo(sampleCryptoContexts.get(0))
                },
                {
                        sampleHybridEncryptionInputs.get(3), "3DES/CBC/PKCS5Padding", "RSA/ECB/OAEPwithSHA512andMGF1Padding", null,
                        sampleCryptoContexts.get(0), hsmBasedKeyResolver.getCertificateInfo(sampleCryptoContexts.get(0))
                }
        };
    }

    @Test(dataProvider = "sampleHybridDecryptDataProvider", priority = 2)
    public void testHybridDecrypt(HybridEncryptionOutput hybridDecryptionInput, String symmetricAlgorithm,
                                  String asymmetricAlgorithm, String javaSecurityProvider, CryptoContext cryptoContext,
                                  PrivateKeyInfo privateKeyInfo, byte[] expectedOutput) throws CryptoException {

        byte[] clearData = hsmBasedExternalCryptoProvider.hybridDecrypt(hybridDecryptionInput,
                symmetricAlgorithm, asymmetricAlgorithm, javaSecurityProvider, cryptoContext, privateKeyInfo);
        Assert.assertEquals(clearData, expectedOutput);
    }

    @DataProvider(name = "sampleHybridDecryptDataProvider")
    public Object[][] getSampleHybridDecryptData() {

        return new Object[][]{
                {
                        sampleHybridEncryptedData.get(0), "AES_192/GCM/NoPadding", "RSA/ECB/OAEPwithSHA1andMGF1Padding", null,
                        sampleCryptoContexts.get(0), hsmBasedKeyResolver.getPrivateKeyInfo(sampleCryptoContexts.get(0)), sampleData.get(0)
                },
                {
                        sampleHybridEncryptedData.get(1), "AES_256/CBC/PKCS5Padding", "RSA/ECB/PKCS1Padding", null,
                        sampleCryptoContexts.get(1), hsmBasedKeyResolver.getPrivateKeyInfo(sampleCryptoContexts.get(1)), sampleData.get(1)
                },
                {
                        sampleHybridEncryptedData.get(2), "DES/CBC/PKCS5Padding", "RSA/ECB/OAEPwithMD5andMGF1Padding", null,
                        sampleCryptoContexts.get(0), hsmBasedKeyResolver.getPrivateKeyInfo(sampleCryptoContexts.get(0)), sampleData.get(2)
                },
                {
                        sampleHybridEncryptedData.get(3), "3DES/CBC/PKCS5Padding", "RSA/ECB/OAEPwithSHA512andMGF1Padding", null,
                        sampleCryptoContexts.get(0), hsmBasedKeyResolver.getPrivateKeyInfo(sampleCryptoContexts.get(0)), sampleData.get(0)
                }
        };
    }
}