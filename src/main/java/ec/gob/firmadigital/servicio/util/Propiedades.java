/*
 * Copyright (C) 2020 
 * Authors: Ricardo Arguello, Misael Fernández
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.*
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package ec.gob.firmadigital.servicio.util;

import ec.gob.firmadigital.libreria.exceptions.HoraServidorException;
import java.io.IOException;
import java.util.Properties;

import ec.gob.firmadigital.libreria.sign.pdf.PDFSignerItext;
import ec.gob.firmadigital.libreria.sign.pdf.RectanguloUtil;
import ec.gob.firmadigital.libreria.utils.TiempoUtils;
import java.io.StringReader;
import java.net.URLDecoder;
import java.util.Base64;
import jakarta.json.Json;
import jakarta.json.JsonReader;

public class Propiedades {

    public static Properties propiedades(String version, String llx, String lly, String pagina, String tipoEstampa, String razon, String url, String fechaHora, String base64) throws IOException, HoraServidorException {
        return getPropiedades(null, llx, lly, pagina, tipoEstampa, razon, url, fechaHora, base64);
    }

    public static Properties propiedades(String llx, String lly, String pagina, String tipoEstampa, String razon, String url, String fechaHora, String base64) throws IOException, HoraServidorException {
        return getPropiedades(null, llx, lly, pagina, tipoEstampa, razon, url, fechaHora, base64);
    }

    public static Properties getPropiedades(String version, String llx, String lly, String pagina, String tipoEstampa, String razon, String url, String fechaHora, String base64) throws IOException, HoraServidorException {
        Properties properties = new Properties();
        properties.setProperty(PDFSignerItext.SIGNING_LOCATION, "");
        if (fechaHora == null) {
            properties.setProperty(PDFSignerItext.SIGN_TIME, TiempoUtils.getFechaHoraServidor(url != null ? url + "/fecha-hora" : null, base64));
        } else {
            properties.setProperty(PDFSignerItext.SIGN_TIME, fechaHora);
        }
        String jsonParameter = new String(Base64.getDecoder().decode(base64));
        jakarta.json.JsonObject json;
        JsonReader jsonReader = Json.createReader(new StringReader(URLDecoder.decode(jsonParameter, "UTF-8")));
        json = (jakarta.json.JsonObject) jsonReader.read();
        String getVersion;
        getVersion = json.getString("versionApp");
        String getSistemaOperativo;
        getSistemaOperativo = json.getString("sistemaOperativo");

        if (version != null) {
            properties.setProperty(PDFSignerItext.INFO_QR, "VALIDAR CON: www.firmadigital.gob.ec\n" + "Firmado digitalmente con FirmaEC mobile " + version + " " + getSistemaOperativo);
        } else {
            properties.setProperty(PDFSignerItext.INFO_QR, "VALIDAR CON: www.firmadigital.gob.ec\n" + "Firmado digitalmente con FirmaEC transversal" + getVersion + " " + getSistemaOperativo);
        }
        if (llx != null) {
            properties.setProperty(RectanguloUtil.POSITION_ON_PAGE_LOWER_LEFT_X, llx);
        }
        if (lly != null) {
            properties.setProperty(RectanguloUtil.POSITION_ON_PAGE_LOWER_LEFT_Y, lly);
        }
        if (pagina != null) {
            properties.setProperty(PDFSignerItext.LAST_PAGE, pagina);
        }
        if (tipoEstampa != null) {
            properties.setProperty(PDFSignerItext.TYPE_SIG, tipoEstampa);
        } else {
            properties.setProperty(PDFSignerItext.TYPE_SIG, "QR");
        }
        if (razon != null) {
            properties.setProperty(PDFSignerItext.SIGNING_REASON, URLDecoder.decode(razon, "UTF-8"));
        }
        return properties;
    }
}
