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
package ec.gob.firmadigital.servicio;

import ec.gob.firmadigital.servicio.exception.ServicioVersionException;
import java.util.logging.Logger;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.validation.constraints.NotNull;

import ec.gob.firmadigital.servicio.model.Version;
import ec.gob.firmadigital.servicio.util.PropertiesUtils;
import ec.gob.firmadigital.libreria.utils.OsUtils;
import jakarta.ejb.EJB;

/**
 * Buscar en una lista de versiones. Esto permite tener el control de los
 * dispositivos que utilizan FirmaEC.
 *
 * @author Christian Espinosa <christian.espinosa@mintel.gob.ec>, Misael
 * Fernández
 */
@Stateless
public class ServicioVersion {
    
    @EJB
    private ServicioLog servicioLog;
    
    @PersistenceContext
    private EntityManager em;
    
    private static final Logger logger = Logger.getLogger(ServicioVersion.class.getName());

    /**
     * Busca una versión, para ello se utiliza el sha con la versión 256
     *
     * @param sistemaOperativo
     * @param aplicacion
     * @param versionApp
     * @param sha
     * @return
     * @throws ServicioVersionException
     */
    public String validarVersion(@NotNull String sistemaOperativo, @NotNull String aplicacion, @NotNull String versionApp, @NotNull String sha) throws ServicioVersionException {
        String retorno = "";
        com.google.gson.JsonObject gsonObject = null;
        try {
            TypedQuery<Version> query = em.createNamedQuery("Version.validarVersion", Version.class);
            query.setParameter("sistema_operativo", OsUtils.getNameOs(sistemaOperativo));
            query.setParameter("aplicacion", aplicacion);
            query.setParameter("version", versionApp);
//            query.setParameter("sha", sha);
            Version version = query.getSingleResult();
            if (version.getStatus()) {
                retorno = "Version enabled";
                servicioLog.info("ServicioVersion::validarVersion",
                        "sistemaOperativo " + sistemaOperativo + ", versionApp " + versionApp + ", sha" + sha + ", " + retorno);
            } else {
                retorno = "Version disabled";
                servicioLog.warning("ServicioVersion::validarVersion",
                        "sistemaOperativo " + sistemaOperativo + ", versionApp " + versionApp + ", sha" + sha + ", " + retorno);
            }
        } catch (NoResultException e) {
            retorno = "Versión no encontrado";
            logger.severe(retorno);
            servicioLog.error("ServicioVersion::validarVersion",
                    "sistemaOperativo " + sistemaOperativo + ", versionApp " + versionApp + ", sha" + sha + ", " + retorno);
            throw new ApiUrlNoEncontradoException(retorno);
        } catch (NonUniqueResultException e) {
            retorno = "Varias Versiones registradas";
            logger.severe(retorno);
            servicioLog.error("ServicioVersion::validarVersion",
                    "sistemaOperativo " + sistemaOperativo + ", versionApp " + versionApp + ", sha" + sha + ", " + retorno);
            throw new ApiUrlNoEncontradoException(retorno);
        } catch (java.lang.NullPointerException e) {
            retorno = "Revisar el estado de la URL registrada";
            logger.severe(retorno);
            servicioLog.error("ServicioVersion::validarVersion",
                    "sistemaOperativo " + sistemaOperativo + ", versionApp " + versionApp + ", sha" + sha + ", " + retorno);
            throw new ApiUrlNoEncontradoException(retorno);
        } finally {
            gsonObject = new com.google.gson.JsonObject();
            gsonObject.addProperty("resultado", retorno);
            if (aplicacion.equals("MOBILE") || aplicacion.equals("LIBRERIA")) {
                gsonObject.addProperty("documentoKB", PropertiesUtils.getDocumentoKB());
            }
            return gsonObject.toString();
        }
    }
}
