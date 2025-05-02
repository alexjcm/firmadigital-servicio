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

/**
 *
 * @author Misael Fernández
 */
public class PropertiesUtils {

    private static final String REQUEST_SIZE_SYSTEM_PROPERTY = "firmadigital-servicio.request.size";

    public static int getDocumentoKB() {
        return (System.getProperty(REQUEST_SIZE_SYSTEM_PROPERTY)) != null
                ? Integer.parseInt(System.getProperty(REQUEST_SIZE_SYSTEM_PROPERTY)) : 512000;//KB
    }
}
