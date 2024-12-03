/*
 * Copyright (C) 2020
 * Authors: Ricardo Arguello, Misael Fernández, Efraín Rodríguez
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
package ec.gob.firmadigital.servicio.rest;

import com.google.gson.Gson;

import com.google.gson.JsonArray;
import ec.gob.firmadigital.libreria.certificate.to.Certificado;
import ec.gob.firmadigital.libreria.certificate.to.Documento;
import ec.gob.firmadigital.libreria.utils.Utils;
import com.google.gson.JsonObject;
import com.itextpdf.kernel.pdf.PdfReader;
import ec.gob.firmadigital.libreria.sign.SignInfo;
import ec.gob.firmadigital.libreria.sign.Signer;
import ec.gob.firmadigital.libreria.sign.pdf.BasePdfSigner;
import java.io.ByteArrayInputStream;

import jakarta.ejb.Stateless;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@Stateless
@Path("/validacionavanzadapdf")
public class ServicioValidacionPdfRest {

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response verificarPdf(String archivoBase64)
            throws Exception {
        byte[] byteDocumento = java.util.Base64.getDecoder().decode(archivoBase64);
        InputStream inputStreamDocumento = new ByteArrayInputStream(byteDocumento);
        PdfReader pdfReader = new PdfReader(inputStreamDocumento);
        Signer signer = new BasePdfSigner();
        java.util.List<SignInfo> signInfos;
        signInfos = signer.getSigners(byteDocumento);

        try {
            Documento documento = Utils.pdfToDocumento(pdfReader, signInfos);
            Gson gson = new Gson();
            JsonObject jsonDoc = new JsonObject();

            if (documento.getError() == null) {
                jsonDoc.addProperty("firmasValidas", documento.getSignValidate());
                jsonDoc.addProperty("integridadDocumento", documento.getDocValidate());
                jsonDoc.addProperty("integridadDocumento", documento.getDocValidate());
                jsonDoc.addProperty("error", "null");
                JsonArray arrayCer = new JsonArray();
                for (Certificado certificado : documento.getCertificados()) {
                    JsonObject jsonCertificado = new JsonObject();
                    jsonCertificado.addProperty("emitidoPara", certificado.getIssuedTo());
                    jsonCertificado.addProperty("emitidoPor", certificado.getIssuedBy());
                    jsonCertificado.addProperty("validoDesde", calendarToString(certificado.getValidFrom()));
                    jsonCertificado.addProperty("validoHasta", calendarToString(certificado.getValidTo()));
                    jsonCertificado.addProperty("fechaFirma", calendarToString(certificado.getSignGenerated()));
                    jsonCertificado.addProperty("fechaRevocado", certificado.getRevocated() != null ? calendarToString(certificado.getRevocated()) : "");
                    jsonCertificado.addProperty("certificadoVigente", certificado.getCertificateValidated());
                    jsonCertificado.addProperty("clavesUso", certificado.getKeyUsages());
                    jsonCertificado.addProperty("fechaSelloTiempo", certificado.getDocTimeStamp() != null ? dateToString(certificado.getDocTimeStamp()) : "");
                    jsonCertificado.addProperty("integridadFirma", certificado.getSignVerify());
                    jsonCertificado.addProperty("razonFirma", certificado.getDocReason() != null ? certificado.getDocReason() : "");
                    jsonCertificado.addProperty("localizacion", certificado.getDocLocation() != null ? certificado.getDocLocation() : "");
                    jsonCertificado.addProperty("cedula", certificado.getDatosUsuario().getCedula());
                    jsonCertificado.addProperty("nombre", certificado.getDatosUsuario().getNombre());
                    jsonCertificado.addProperty("apellido", certificado.getDatosUsuario().getApellido());
                    jsonCertificado.addProperty("institucion", certificado.getDatosUsuario().getInstitucion());
                    jsonCertificado.addProperty("cargo", certificado.getDatosUsuario().getCargo());
                    jsonCertificado.addProperty("entidadCertificadora", certificado.getIssuedBy());
                    jsonCertificado.addProperty("serial", certificado.getSerial());
                    jsonCertificado.addProperty("selladoTiempo", certificado.getDocValidTimeStamp());
                    jsonCertificado.addProperty("certificadoDigitalValido", certificado.getDatosUsuario().isCertificadoDigitalValido());
                    arrayCer.add(jsonCertificado);
                }
                jsonDoc.add("certificado", arrayCer);
                String json = gson.toJson(jsonDoc);

                return Response.ok(json, MediaType.APPLICATION_JSON).build();

            } else {
                jsonDoc.addProperty("firmasValidas", false);
                jsonDoc.addProperty("integridadDocumento", false);
                jsonDoc.addProperty("error", documento.getError());
                String json = gson.toJson(jsonDoc);

                return Response.ok(json, MediaType.APPLICATION_JSON).build();
            }
        } catch (Exception exception) {
            Gson gson = new Gson();
            JsonObject jsonDoc = new JsonObject();
            jsonDoc.addProperty("firmasValidas", false);
            jsonDoc.addProperty("integridadDocumento", false);
            jsonDoc.addProperty("error", "El archivo no pudo ser validado o no es un PDF");
            String json = gson.toJson(jsonDoc);

            return Response.status(Status.BAD_REQUEST).entity(json).build();
        }
    }

    private String calendarToString(Calendar calendar) {
        Date date = calendar.getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(date);
    }

    private String dateToString(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(date);
    }

}
