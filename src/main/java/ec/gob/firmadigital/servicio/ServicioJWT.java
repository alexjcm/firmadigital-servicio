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

import ec.gob.firmadigital.servicio.exception.ServicioSistemaTransversalException;
import ec.gob.firmadigital.servicio.token.ServicioToken;

import jakarta.ejb.Stateless;
import jakarta.validation.constraints.NotNull;

import ec.gob.firmadigital.servicio.token.TokenTimeout;
import ec.gob.firmadigital.servicio.util.UtilsJson;
import jakarta.ejb.EJB;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestiona Estándar JWT.
 *
 * @author Misael Fernández
 */
@Stateless
public class ServicioJWT {

    @EJB
    private ServicioToken servicioToken;

    @EJB
    private ServicioSistemaTransversal servicioSistemaTransversal;

    /**
     * genera un token bajo el estándar JWT
     *
     * @param apiKey
     * @param sistemaTransversal
     * @return jwt
     * @throws
     * ec.gob.firmadigital.servicio.exception.ServicioSistemaTransversalException
     */
    public String getJWT(@NotNull String apiKey, @NotNull String sistemaTransversal) throws ServicioSistemaTransversalException {
        if (servicioSistemaTransversal.verificarApiKey(sistemaTransversal, apiKey)) {
            Map<String, Object> parametros = new HashMap<>();
            if (apiKey.equals(apiKey)) {
                parametros.put("sistema", sistemaTransversal);
            }
            // Expiracion del Token
            Date expiracion = TokenTimeout.addSeconds(new Date(), 5);//segundos
            // Retorna el Token
            return UtilsJson.generarJsonResponse(
                    200, 
                    null, 
                    servicioToken.generarToken(parametros, expiracion));
        } else {
            return UtilsJson.generarJsonResponse(
                    500, 
                    "La información enviada no concuerda con la registrada en FirmaEC", 
                    null);
        }
    }
}
