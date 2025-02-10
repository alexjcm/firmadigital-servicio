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
package ec.gob.firmadigital.servicio.crl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.EJBException;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import javax.sql.DataSource;
import ec.gob.firmadigital.libreria.crl.ServicioCRL;
import ec.gob.firmadigital.libreria.utils.HttpClient;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Servicio para cargar los CRLs de las CAs soportadas en una tabla.
 *
 * @author Ricardo Arguello <ricardo.arguello@soportelibre.com>
 */
@Singleton
//GRANJA DE SERVIDORES EN PRODUCCION - COMENTAR EVITAR DESCARGA CRL
//@Startup
//GRANJA DE SERVIDORES EN PRODUCCION - COMENTAR EVITAR DESCARGA CRL
public class ServicioDescargaCrl {

    @Resource(lookup = "java:/FirmaDigitalDS")
    private DataSource ds;

    private static final Logger LOGGER = Logger.getLogger(ServicioDescargaCrl.class.getName());

    //GRANJA DE SERVIDORES EN PRODUCCION - COMENTAR EVITAR DESCARGA CRL
//    @PostConstruct
//    public void init() {
//        crearTablaSiNoExiste();
//        importarCrls();
//    }
//
//    //10 segundos
//    //@Schedule(hour = "*", minute = "*", second = "*/10", persistent = false)
//    //5 minutos
//    @Schedule(hour = "*", minute = "*/5", persistent = false)
//    //1 hora
//    //@Schedule(minute = "0", hour = "*", persistent = false)
    //GRANJA DE SERVIDORES EN PRODUCCION - COMENTAR EVITAR DESCARGA CRL

    public void importarCrls() {
        LOGGER.info("Iniciando el proceso de descarga de CRL");

        LOGGER.info("Descargando CRL de BCE...");
        X509CRL bceCrl = downloadCrl(ServicioCRL.BCE_CRL);

        LOGGER.info("Descargando CRL de Security Data 1...");
        X509CRL sdCrl1 = downloadCrl(ServicioCRL.SD_CRL1);

        LOGGER.info("Descargando CRL de Security Data 2...");
        X509CRL sdCrl2 = downloadCrl(ServicioCRL.SD_CRL2);

        LOGGER.info("Descargando CRL de Security Data 3...");
        X509CRL sdCrl3 = downloadCrl(ServicioCRL.SD_CRL3);

        LOGGER.info("Descargando CRL de Security Data 4...");
        X509CRL sdCrl4 = downloadCrl(ServicioCRL.SD_CRL4);

        LOGGER.info("Descargando CRL de Security Data 5...");
        X509CRL sdCrl5 = downloadCrl(ServicioCRL.SD_CRL5);

        LOGGER.info("Descargando CRL de CJ...");
        X509CRL cjCrl = downloadCrl(ServicioCRL.CJ_CRL);

        LOGGER.info("Descargando CRL de ANFAC1...");
        X509CRL anfAcCrl1 = downloadCrl(ServicioCRL.ANFAC_CRL1);

        LOGGER.info("Descargando CRL de ANFAC2...");
        X509CRL anfAcCrl2 = downloadCrl(ServicioCRL.ANFAC_CRL2);

        LOGGER.info("Descargando CRL de DIGERCIC...");
        X509CRL digercicCrl = downloadCrl(ServicioCRL.DIGERCIC_CRL);

        LOGGER.info("Descargando CRL de UANATACA1...");
        X509CRL uanatacaCrl1 = downloadCrl(ServicioCRL.UANATACA_CRL1);

        LOGGER.info("Descargando CRL de UANATACA2...");
        X509CRL uanatacaCrl2 = downloadCrl(ServicioCRL.UANATACA_CRL2);

        LOGGER.info("Descargando CRL de DATIL...");
        X509CRL datilCrl = downloadCrl(ServicioCRL.DATIL_CRL);

        LOGGER.info("Descargando CRL de ARGOSDATA...");
        X509CRL argosDataCrl = downloadCrl(ServicioCRL.ARGOSDATA_CRL);

        LOGGER.info("Descargando CRL de LAZZATE...");
        X509CRL lazzateCrl = downloadCrl(ServicioCRL.LAZZATE_CRL);

        LOGGER.info("Descargando CRL de ALPHATECHNOLOGIES...");
        X509CRL alphaTechnologiesCrl = downloadCrl(ServicioCRL.ALPHATECHNOLOGIES_CRL);

        LOGGER.info("Descargando CRL de CORPNEWBEST 1...");
        X509CRL corpNewBestCrl1 = downloadCrl(ServicioCRL.CORPNEWBEST_CRL1);

        LOGGER.info("Descargando CRL de CORPNEWBEST 2...");
        X509CRL corpNewBestCrl2 = downloadCrl(ServicioCRL.CORPNEWBEST_CRL2);

        LOGGER.info("Descargando CRL de CORPNEWBEST 3...");
        X509CRL corpNewBestCrl3 = downloadCrl(ServicioCRL.CORPNEWBEST_CRL3);

        LOGGER.info("Descargando CRL de FIRMA SEGURA...");
        X509CRL firmaSeguraCrl = downloadCrl(ServicioCRL.FIRMASEGURA_CRL);

        LOGGER.info("Descargando CRL de LAZZATECA1...");
        X509CRL lazzateCa1Crl = downloadCrl(ServicioCRL.LAZZATECA1_CRL);

        LOGGER.info("Descargando CRL de LAZZATECA2...");
        X509CRL lazzateCa2Crl = downloadCrl(ServicioCRL.LAZZATECA2_CRL);

        LOGGER.info("Descargando CRL de LAZZATEWEGO...");
        X509CRL lazzateCaWeGoCrl = downloadCrl(ServicioCRL.LAZZATE_WE_GO_CRL);

        try (Connection conn = ds.getConnection(); PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO crl (serial, fecharevocacion, razonrevocacion, entidadcertificadora) VALUES (?,?,?,?) "
                + "ON CONFLICT (serial) "
                + "DO UPDATE SET fecharevocacion = EXCLUDED.fecharevocacion, razonrevocacion = EXCLUDED.razonrevocacion, entidadcertificadora = EXCLUDED.entidadcertificadora")) {

            LOGGER.info("Iniciando actualizacion de CRLs...");

            int contadorBCE = 0;
            int contadorSD1 = 0, contadorSD2 = 0, contadorSD3 = 0, contadorSD4 = 0, contadorSD5 = 0;
            int contadorCJ = 0;
            int contadorANFAC1 = 0;
            int contadorANFAC2 = 0;
            int contadorDIGERCIC = 0;
            int contadorUANATACA1 = 0, contadorUANATACA2 = 0;
            int contadorDATIL = 0;
            int contadorARGOSDATA = 0;
            int contadorLAZZATE = 0;
            int contadorALPHATECHNOLOGIES = 0;
            int contadorCorpNewBest1 = 0;
            int contadorCorpNewBest2 = 0;
            int contadorCorpNewBest3 = 0;
            int contadorFirmaSegura = 0;
            int contadorLazzateCa1 = 0;
            int contadorLazzateCa2 = 0;
            int contadorLazzateCaWeGo = 0;

            if (bceCrl != null) {
                contadorBCE = insertarCrl(bceCrl, 1, ps);
                LOGGER.log(Level.INFO, "Registros insertados/actualizados BCE (1): {0}", contadorBCE);
            } else {
                LOGGER.info("No se inserta BCE (1)");
            }

            if (sdCrl1 != null) {
                contadorSD1 = insertarCrl(sdCrl1, 2, ps);
                LOGGER.log(Level.INFO, "Registros insertados/actualizados Security Data 1 (2): {0}", contadorSD1);
            } else {
                LOGGER.info("No se inserta Security Data 1 (2)");
            }

            if (sdCrl2 != null) {
                contadorSD2 = insertarCrl(sdCrl2, 2, ps);
                LOGGER.log(Level.INFO, "Registros insertados/actualizados Security Data 2 (2): {0}", contadorSD2);
            } else {
                LOGGER.info("No se inserta Security Data 2 (2)");
            }

            if (sdCrl3 != null) {
                contadorSD3 = insertarCrl(sdCrl3, 2, ps);
                LOGGER.log(Level.INFO, "Registros insertados/actualizados Security Data 3 (2): {0}", contadorSD3);
            } else {
                LOGGER.info("No se inserta Security Data 3 (2)");
            }

            if (sdCrl4 != null) {
                contadorSD4 = insertarCrl(sdCrl4, 2, ps);
                LOGGER.log(Level.INFO, "Registros insertados/actualizados Security Data 4 (2): {0}", contadorSD4);
            } else {
                LOGGER.info("No se inserta Security Data 4 (2)");
            }

            if (sdCrl5 != null) {
                contadorSD5 = insertarCrl(sdCrl5, 2, ps);
                LOGGER.log(Level.INFO, "Registros insertados/actualizados Security Data 5 (2): {0}", contadorSD5);
            } else {
                LOGGER.info("No se inserta Security Data 5 (2)");
            }

            if (cjCrl != null) {
                contadorCJ = insertarCrl(cjCrl, 3, ps);
                LOGGER.log(Level.INFO, "Registros insertados/actualizados CJ (3): {0}", contadorCJ);
            } else {
                LOGGER.info("No se inserta CJ (3)");
            }

            if (anfAcCrl1 != null) {
                contadorANFAC1 = insertarCrl(anfAcCrl1, 4, ps);
                LOGGER.log(Level.INFO, "Registros insertados/actualizados ANFAC1 (4): {0}", contadorANFAC1);
            } else {
                LOGGER.info("No se inserta ANFAC1 (4)");
            }

            if (anfAcCrl2 != null) {
                contadorANFAC2 = insertarCrl(anfAcCrl2, 4, ps);
                LOGGER.log(Level.INFO, "Registros insertados/actualizados ANFAC2 (4): {0}", contadorANFAC2);
            } else {
                LOGGER.info("No se inserta ANFAC2 (4)");
            }

            if (uanatacaCrl1 != null) {
                contadorUANATACA1 = insertarCrl(uanatacaCrl1, 5, ps);
                LOGGER.log(Level.INFO, "Registros insertados/actualizados UANATACA 1 (5): {0}", contadorUANATACA1);
            } else {
                LOGGER.info("No se inserta UANATACA 1 (5)");
            }

            if (uanatacaCrl2 != null) {
                contadorUANATACA2 = insertarCrl(uanatacaCrl2, 6, ps);
                LOGGER.log(Level.INFO, "Registros insertados/actualizados UANATACA 2 (6): {0}", contadorUANATACA2);
            } else {
                LOGGER.info("No se inserta UANATACA 2 (6)");
            }

            if (digercicCrl != null) {
                contadorDIGERCIC = insertarCrl(digercicCrl, 7, ps);
                LOGGER.log(Level.INFO, "Registros insertados/actualizados DIGERCIC (7): {0}", contadorDIGERCIC);
            } else {
                LOGGER.info("No se inserta DIGERCIC (7)");
            }

            if (datilCrl != null) {
                contadorDATIL = insertarCrl(datilCrl, 8, ps);
                LOGGER.log(Level.INFO, "Registros insertados/actualizados DATIL (8): {0}", contadorDATIL);
            } else {
                LOGGER.info("No se inserta DATIL (8)");
            }

            if (argosDataCrl != null) {
                contadorARGOSDATA = insertarCrl(argosDataCrl, 9, ps);
                LOGGER.log(Level.INFO, "Registros insertados/actualizados ARGOSDATA (9): {0}", contadorARGOSDATA);
            } else {
                LOGGER.info("No se inserta ARGOSDATA (9)");
            }

            if (lazzateCrl != null) {
                contadorLAZZATE = insertarCrl(lazzateCrl, 10, ps);
                LOGGER.log(Level.INFO, "Registros insertados/actualizados LAZZATE (10): {0}", contadorLAZZATE);
            } else {
                LOGGER.info("No se inserta LAZZATE (10)");
            }

            if (alphaTechnologiesCrl != null) {
                contadorALPHATECHNOLOGIES = insertarCrl(alphaTechnologiesCrl, 11, ps);
                LOGGER.log(Level.INFO, "Registros insertados/actualizados ALPHATECHNOLOGIES (11): {0}", contadorALPHATECHNOLOGIES);
            } else {
                LOGGER.info("No se inserta ALPHATECHNOLOGIES (11)");
            }

            if (corpNewBestCrl1 != null) {
                contadorCorpNewBest1 = insertarCrl(corpNewBestCrl1, 12, ps);
                LOGGER.log(Level.INFO, "Registros insertados/actualizados CORPNEWBEST 1 (12): {0}", contadorCorpNewBest1);
            } else {
                LOGGER.info("No se inserta CORPNEWBEST 1 (12)");
            }

            if (corpNewBestCrl2 != null) {
                contadorCorpNewBest2 = insertarCrl(corpNewBestCrl2, 13, ps);
                LOGGER.log(Level.INFO, "Registros insertados/actualizados CORPNEWBEST 2 (13): {0}", contadorCorpNewBest2);
            } else {
                LOGGER.info("No se inserta CORPNEWBEST 2 (13)");
            }

            if (corpNewBestCrl3 != null) {
                contadorCorpNewBest3 = insertarCrl(corpNewBestCrl3, 14, ps);
                LOGGER.log(Level.INFO, "Registros insertados/actualizados CORPNEWBEST 3 (14): {0}", contadorCorpNewBest3);
            } else {
                LOGGER.info("No se inserta CORPNEWBEST 3 (14)");
            }

            if (firmaSeguraCrl != null) {
                contadorFirmaSegura = insertarCrl(firmaSeguraCrl, 15, ps);
                LOGGER.log(Level.INFO, "Registros insertados/actualizados FIRMA SEGURA (15): {0}", contadorFirmaSegura);
            } else {
                LOGGER.info("No se inserta FIRMA SEGURA (15)");
            }

            if (lazzateCa1Crl != null) {
                contadorLazzateCa1 = insertarCrl(lazzateCa1Crl, 16, ps);
                LOGGER.log(Level.INFO, "Registros insertados/actualizados LAZZATECA1 (16): {0}", contadorLazzateCa1);
            } else {
                LOGGER.info("No se inserta LAZZATECA1 (16)");
            }

            if (lazzateCa2Crl != null) {
                contadorLazzateCa2 = insertarCrl(lazzateCa2Crl, 17, ps);
                LOGGER.log(Level.INFO, "Registros insertados/actualizados LAZZATECA2 (17): {0}", contadorLazzateCa2);
            } else {
                LOGGER.info("No se inserta LAZZATECA2 (17)");
            }

            if (lazzateCaWeGoCrl != null) {
                contadorLazzateCaWeGo = insertarCrl(lazzateCaWeGoCrl, 18, ps);
                LOGGER.log(Level.INFO, "Registros insertados/actualizados LAZZATECAWEGO (18): {0}", contadorLazzateCaWeGo);
            } else {
                LOGGER.info("No se inserta LAZZATECAWEGO (18)");
            }

            int total = contadorBCE + contadorSD1 + contadorSD2 + contadorSD3
                    + contadorSD4 + contadorSD5 + contadorCJ + contadorANFAC1
                    + contadorANFAC2 + contadorUANATACA1 + contadorUANATACA2
                    + contadorDIGERCIC + contadorDATIL + contadorARGOSDATA
                    + contadorLAZZATE + contadorALPHATECHNOLOGIES
                    + contadorCorpNewBest1 + contadorCorpNewBest2 + contadorCorpNewBest3
                    + contadorFirmaSegura + contadorLazzateCa1 + contadorLazzateCa2
                    + contadorLazzateCaWeGo;
            LOGGER.log(Level.INFO, "Registros insertados/actualizados Total: {0}", total);

            LOGGER.info("Finalizado!");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al insertar/actualizar certificados revocados", e);
            throw new EJBException(e);
        }
    }

    private int insertarCrl(X509CRL crl, int entidadCertificadora, PreparedStatement ps) throws SQLException {
        // Existen CRLs?
        if (crl.getRevokedCertificates() == null) {
            return 0;
        }

        for (X509CRLEntry cert : crl.getRevokedCertificates()) {
            BigInteger serial = cert.getSerialNumber();
            Date fechaRevocacion = cert.getRevocationDate();
            String razonRevocacion = cert.getRevocationReason() == null ? "" : cert.getRevocationReason().toString();
            LocalDateTime ldt = LocalDateTime.ofInstant(fechaRevocacion.toInstant(), ZoneId.systemDefault());

            //https://www.ipa.go.jp/security/rfc/RFC3280-04EN.html#41202
            Pattern pattern = Pattern.compile("\\d{1,2000}");
            Matcher matcher = pattern.matcher(serial.toString());
            if (matcher.matches()) {
                ps.setString(1, serial.toString());
                ps.setObject(2, ldt);
                ps.setString(3, razonRevocacion);
                ps.setInt(4, entidadCertificadora);
                ps.addBatch();
            } else {
                LOGGER.log(Level.SEVERE, "Error con el serial number {0} de la entidad certificadora {1}", new Object[]{serial.toString(), entidadCertificadora});
            }
        }

        int[] count = ps.executeBatch();
        return count.length;
    }

    private X509CRL downloadCrl(String url) {
        byte[] content;

        try {
            HttpClient http = new HttpClient();
            content = http.download(url);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al descargar CRL de {0}: {1}", new Object[]{url, e.getMessage()});
            return null;
        }

        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509CRL) cf.generateCRL(new ByteArrayInputStream(content));
        } catch (CertificateException | CRLException e) {
            LOGGER.log(Level.SEVERE, "Error al descargar CRL de {0}: {1}", new Object[]{url, e.getMessage()});
            return null;
        }
    }

    private void crearTablaSiNoExiste() {
        LOGGER.info("Creando tabla CRL si es que no existe...");

        try (Connection conn = ds.getConnection(); Statement st = conn.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS crl ("
                    + "serial varchar(2000) NOT NULL, "
                    + "fecharevocacion varchar(2000) NULL, "
                    + "razonrevocacion varchar(2000) NULL, "
                    + "entidadcertificadora varchar(2000) NULL,	"
                    + "CONSTRAINT pk_serial PRIMARY KEY (serial))");
            LOGGER.info("Tabla CRL creada");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al crear tabla CRL", e);
            throw new EJBException(e);
        }
    }
}
