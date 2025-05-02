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

import static ec.gob.firmadigital.servicio.token.TokenTimeout.DEFAULT_TIMEOUT_HOURS;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.EJBException;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import javax.sql.DataSource;

/**
 * Servicio para eliminar documentos de la base de datos que no han sido
 * firmados por n minutos.
 *
 * @author Ricardo Arguello
 */
@Singleton
//GRANJA DE SERVIDORES EN PRODUCCION - COMENTAR EVITAR ELIMINAR DOCUMENTOS
@Startup
//GRANJA DE SERVIDORES EN PRODUCCION - COMENTAR EVITAR ELIMINAR DOCUMENTOS
public class ServicioEliminacionDocumento {

    @Resource(lookup = "java:/FirmaDigitalDS")
    private DataSource ds;

    // Timeout en minutos
    private static final String TIMEOUT = "5";

    private static final Logger LOGGER = Logger.getLogger(ServicioEliminacionDocumento.class.getName());

    //GRANJA DE SERVIDORES EN PRODUCCION - COMENTAR EVITAR ELIMINAR DOCUMENTOS
    @PostConstruct
    public void init() {
        borrarDocumentos();
    }
    // @Schedule(hour = "*", minute = "*/" + TIMEOUT, persistent = false)
        //To run on every Monday at 9 am
    @Schedule(dayOfWeek = "Mon", hour = "9", persistent = false)
    //GRANJA DE SERVIDORES EN PRODUCCION - COMENTAR EVITAR ELIMINAR DOCUMENTOS
    public void borrarDocumentos() {
        Connection conn = null;
        Statement st = null;
        try {
            conn = ds.getConnection();
            st = conn.createStatement();
			LOGGER.info("Borrando documentos de hace mas de " + DEFAULT_TIMEOUT_HOURS + " horas...");
            int n = st.executeUpdate("DELETE FROM documento WHERE fecha < NOW() - INTERVAL '" + DEFAULT_TIMEOUT_HOURS + " hours'");
            LOGGER.log(Level.INFO, "Registros eliminados: {0}", n);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al borrar documentos", e);
            throw new EJBException(e);
        } finally {
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException e) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }
    }
}
