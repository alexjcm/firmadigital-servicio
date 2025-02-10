/*
 * Firma Digital: Servicio
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
package ec.gob.firmadigital.servicio;

import java.security.MessageDigest;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.xml.bind.DatatypeConverter;
import ec.gob.firmadigital.servicio.exception.ServicioSistemaMobileException;
import ec.gob.firmadigital.servicio.model.SistemaMobile;

/**
 * Servicio para invocar Web Services de los sistemas transaccionales, utilizado
 * para almacenar el documento ya firmado.
 *
 * @author Ricardo Arguello <ricardo.arguello@soportelibre.com>
 */
@Stateless
public class ServicioSistemaMobile {

    @PersistenceContext(unitName = "FirmaDigitalDS")
    private EntityManager em;

    private static final Logger LOGGER = Logger.getLogger(ServicioSistemaMobile.class.getName());

    /**
     * Buscar un sistema mobile.
     *
     * @param nombre
     * @return
     * @throws
     * ec.gob.firmadigital.servicio.exception.ServicioSistemaMobileException
     */
    private SistemaMobile buscarSistemaMobile(String nombre) throws ServicioSistemaMobileException {
        try {
            TypedQuery<SistemaMobile> q = em.createQuery("SELECT sm FROM SistemaMobile sm WHERE sm.nombre = :nombre", SistemaMobile.class);
            q.setParameter("nombre", nombre);
            return q.getSingleResult();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServicioSistemaMobileException("No se encontro el sistema mobile " + nombre, e.getCause());
        }
    }

    public boolean verificarApiKeyMobile(String nombre, String apiKey) {
        // Verificar si existe SistemaMobile
        SistemaMobile sistemaMobile;

        try {
            sistemaMobile = buscarSistemaMobile(nombre);
        } catch (ServicioSistemaMobileException e) {
            LOGGER.log(Level.SEVERE, "No existe el sistema mobile: {0}", nombre);
            return false;
        }

        String apiKeySistemaMobile = sistemaMobile.getApiKey().toUpperCase();
        LOGGER.log(Level.FINE, "apiKeySistema={0}", apiKey);

        // Si no tiene API KEY
        if (apiKeySistemaMobile == null) {
            LOGGER.log(Level.WARNING, "API KEY MOBILE is null, sistema={0}", nombre);
            return false;
        }

        // Si no tiene problemas el API KEY
        if (!apiKeySistemaMobile.equals(hashSha256(apiKey).toUpperCase())) {
            LOGGER.log(Level.WARNING, "API KEY MOBILE tiene problemas");
            return false;
        }
        return true;
    }

    private String hashSha256(String apiKey) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(apiKey.getBytes("UTF-8"));
            byte[] digest = md.digest();
            return DatatypeConverter.printHexBinary(digest).toLowerCase();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
