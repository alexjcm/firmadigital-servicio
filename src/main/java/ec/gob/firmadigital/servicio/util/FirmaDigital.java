/*
 * Firma Digital: Servicio
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ec.gob.firmadigital.servicio.util;

import com.itextpdf.kernel.crypto.BadPasswordException;
import ec.gob.firmadigital.libreria.exceptions.CertificadoInvalidoException;
import ec.gob.firmadigital.libreria.exceptions.ConexionException;
import ec.gob.firmadigital.libreria.exceptions.DocumentoException;
import ec.gob.firmadigital.libreria.exceptions.EntidadCertificadoraNoValidaException;
import ec.gob.firmadigital.libreria.exceptions.HoraServidorException;
import ec.gob.firmadigital.libreria.exceptions.RubricaException;
import ec.gob.firmadigital.libreria.exceptions.SignatureVerificationException;
import ec.gob.firmadigital.libreria.model.Document;
import ec.gob.firmadigital.libreria.model.InMemoryDocument;
import ec.gob.firmadigital.libreria.sign.DigestAlgorithm;
import ec.gob.firmadigital.libreria.sign.PrivateKeySigner;
import ec.gob.firmadigital.libreria.sign.SignConstants;
import ec.gob.firmadigital.libreria.sign.pdf.PadesBasicSigner;
import ec.gob.firmadigital.libreria.sign.xades.XAdESSigner;
import ec.gob.firmadigital.libreria.utils.X509CertificateUtils;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FirmaDigital {

    private final String HASH_ALGORITHM = "SHA512";
    private static final Logger LOGGER = Logger.getLogger(ec.gob.firmadigital.servicio.ServicioAppFirmarDocumento.class.getName());

    /**
     * Firmar un documento PDF usando un KeyStore y una clave.
     *
     * @param keyStore
     * @param alias
     * @param docByteArry
     * @param keyStorePassword
     * @param properties
     * @param api
     * @param base64
     * @return
     * @throws java.security.InvalidKeyException
     * @throws
     * ec.gob.firmadigital.libreria.exceptions.EntidadCertificadoraNoValidaException
     * @throws java.io.IOException
     * @throws ec.gob.firmadigital.libreria.exceptions.HoraServidorException
     * @throws java.security.UnrecoverableKeyException
     * @throws java.security.KeyStoreException
     * @throws
     * ec.gob.firmadigital.libreria.exceptions.CertificadoInvalidoException
     * @throws ec.gob.firmadigital.libreria.exceptions.ConexionException
     * @throws java.security.NoSuchAlgorithmException
     * @throws ec.gob.firmadigital.libreria.exceptions.RubricaException
     * @throws
     * ec.gob.firmadigital.libreria.exceptions.SignatureVerificationException
     * @throws ec.gob.firmadigital.libreria.exceptions.DocumentoException
     */
    public byte[] firmarPDF(KeyStore keyStore, String alias, byte[] docByteArry, char[] keyStorePassword, Properties properties, String api, String base64) throws
            BadPasswordException,
            InvalidKeyException,
            EntidadCertificadoraNoValidaException,
            HoraServidorException,
            UnrecoverableKeyException,
            KeyStoreException,
            CertificadoInvalidoException,
            IOException,
            NoSuchAlgorithmException,
            RubricaException,
            SignatureVerificationException,
            DocumentoException,
            ConexionException {
        byte[] signed = null;
        X509CertificateUtils x509CertificateUtils = new X509CertificateUtils();
        try {
            PrivateKey key = (PrivateKey) keyStore.getKey(alias, keyStorePassword);
            Certificate[] certChain = keyStore.getCertificateChain(alias);
            if (x509CertificateUtils.validarX509Certificate((X509Certificate) keyStore.getCertificate(alias), api, base64)) {//validación de firmaEC
                Document document = new InMemoryDocument(docByteArry);
                try (InputStream is = document.openStream()) {
                    // Crear un RubricaSigner para firmar el MessageDigest del documento
                    PrivateKeySigner signer = new PrivateKeySigner(key, DigestAlgorithm.forName(HASH_ALGORITHM));
                    // Crear un PdfSigner para firmar el documento
                    PadesBasicSigner pdfSigner = new PadesBasicSigner(signer);
                    // Firmar el documento
                    signed = pdfSigner.sign(is, key, certChain, properties);
                } catch (com.itextpdf.io.IOException ioe) {
                    throw new DocumentoException("El archivo no es PDF");
                }
            } else {
                throw new CertificadoInvalidoException(x509CertificateUtils.getError());
            }
            if (x509CertificateUtils.getError() != null) {
                throw new SignatureVerificationException(x509CertificateUtils.getError());
            }
        } catch (IOException ioe) {
            throw new DocumentoException("El archivo no es PDF");
        } catch (DocumentoException de) {
            throw new DocumentoException("El archivo no es PDF");
        } catch (SignatureVerificationException sve) {
            throw new SignatureVerificationException(x509CertificateUtils.getError());
        } catch (CertificadoInvalidoException cie) {
            throw new CertificadoInvalidoException(x509CertificateUtils.getError());
        } catch (Exception e) {
            if (e.getClass() == IllegalArgumentException.class) {
                LOGGER.log(Level.WARNING, "Problemas con la emisión del certificado digital");
            } else {
                e.printStackTrace();
            }
        }
        return signed;
    }

    /**
     * Firmar un documento XML usando un KeyStore y una clave.
     *
     * @param keyStore
     * @param alias
     * @param docByteArry
     * @param keyStorePassword
     * @param properties
     * @param api
     * @param base64
     * @return
     * @throws java.security.InvalidKeyException
     * @throws java.security.UnrecoverableKeyException
     * @throws
     * ec.gob.firmadigital.libreria.exceptions.EntidadCertificadoraNoValidaException
     * @throws ec.gob.firmadigital.libreria.exceptions.HoraServidorException
     * @throws java.security.KeyStoreException
     * @throws java.io.IOException
     * @throws
     * ec.gob.firmadigital.libreria.exceptions.CertificadoInvalidoException
     * @throws java.security.NoSuchAlgorithmException
     * @throws ec.gob.firmadigital.libreria.exceptions.RubricaException
     * @throws
     * ec.gob.firmadigital.libreria.exceptions.SignatureVerificationException
     * @throws ec.gob.firmadigital.libreria.exceptions.ConexionException
     */
    public byte[] firmarXML(KeyStore keyStore, String alias, byte[] docByteArry, char[] keyStorePassword, Properties properties, String api, String base64) throws
            BadPasswordException,
            InvalidKeyException,
            EntidadCertificadoraNoValidaException,
            HoraServidorException,
            UnrecoverableKeyException,
            KeyStoreException,
            CertificadoInvalidoException,
            IOException,
            NoSuchAlgorithmException,
            RubricaException,
            CertificadoInvalidoException,
            SignatureVerificationException,
            ConexionException {
        byte[] signed = null;
        PrivateKey key = (PrivateKey) keyStore.getKey(alias, keyStorePassword);
        Certificate[] certChain = keyStore.getCertificateChain(alias);
        X509CertificateUtils x509CertificateUtils = new X509CertificateUtils();
        try {
            if (x509CertificateUtils.validarX509Certificate((X509Certificate) keyStore.getCertificate(alias), api, base64)) {//validación de firmaEC
                XAdESSigner signer = new XAdESSigner();
                signed = signer.sign(docByteArry, SignConstants.SIGN_ALGORITHM_SHA512WITHRSA, key, certChain, null, base64);
            } else {
                throw new CertificadoInvalidoException(x509CertificateUtils.getError());
            }
            if (x509CertificateUtils.getError() != null) {
                throw new SignatureVerificationException(x509CertificateUtils.getError());
            }
        } catch (SignatureVerificationException sve) {
            throw new SignatureVerificationException(x509CertificateUtils.getError());
        } catch (CertificadoInvalidoException cie) {
            throw new CertificadoInvalidoException(x509CertificateUtils.getError());
        } catch (Exception e) {
            if (e.getClass() == IllegalArgumentException.class) {
                LOGGER.log(Level.WARNING, "Problemas con la emisión del certificado digital");
            } else {
                e.printStackTrace();
            }
        }
        return signed;
    }
}
