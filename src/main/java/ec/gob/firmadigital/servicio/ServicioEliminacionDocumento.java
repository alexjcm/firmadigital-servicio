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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TimerService;
import javax.sql.DataSource;

/**
 * Servicio para eliminar documentos de la base de datos que no han sido
 * firmados por n minutos.
 *
 * @author Ricardo Arguello <ricardo.arguello@soportelibre.com>
 */
@Singleton
@Startup
public class ServicioEliminacionDocumento {

    @Resource
    private TimerService timerService;

    @Resource(lookup = "java:/FirmaDigitalDS")
    private DataSource ds;

    // Timeout en minutos
    //private static final String TIMEOUT = "5";
    
    // Timeout en horas
    private static final String TIMEOUT_HOURS = "360";

    private static final Logger logger = Logger.getLogger(ServicioEliminacionDocumento.class.getName());

    @PostConstruct
    public void init() {
        borrarDocumentos();
    }
    
    // To run every n minutes
    // @Schedule(hour = "*", minute = "*/" + TIMEOUT, persistent = false)
    
    // To run on every Monday at 7 am   
    // @Schedule(dayOfWeek = "Mon", hour = "7", persistent = false)
    
    // To run on the first day of every month at 9 am
    // @Schedule(dayOfMonth="1", hour = "9", persistent = false)

    @Schedule(dayOfWeek = "Mon", hour = "9", persistent = false)
    public void borrarDocumentos() {
        Connection conn = null;
        Statement st = null;

        try {
            conn = ds.getConnection();
            st = conn.createStatement();

            // logger.info("Borrando documentos de hace mas de " + TIMEOUT + " minutos...");
            logger.info("Borrando documentos de hace mas de " + TIMEOUT_HOURS + " horas...");
            // int n = st.executeUpdate("DELETE FROM documento WHERE fecha < NOW() - INTERVAL '" + TIMEOUT + " minutes'");
            int n = st.executeUpdate("DELETE FROM documento WHERE fecha < NOW() - INTERVAL '" + TIMEOUT_HOURS + " hours'");
            logger.log(Level.INFO, "Registros eliminados: {0}", n);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al borrar documentos", e);
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
