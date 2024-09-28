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
package ec.gob.firmadigital.servicio.rest;

import java.io.StringReader;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.stream.JsonParsingException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import ec.gob.firmadigital.servicio.CedulaInvalidaException;
import ec.gob.firmadigital.servicio.CertificadoRevocadoException;
import ec.gob.firmadigital.servicio.DocumentoNoExisteException;
import ec.gob.firmadigital.servicio.ServicioDocumento;
import ec.gob.firmadigital.servicio.ServicioLog;
import ec.gob.firmadigital.servicio.ServicioSistemaTransversal;
import ec.gob.firmadigital.servicio.token.TokenExpiradoException;
import ec.gob.firmadigital.servicio.token.TokenInvalidoException;
import ec.gob.firmadigital.servicio.exception.Base64InvalidoException;
import ec.gob.firmadigital.servicio.exception.ServicioSistemaTransversalException;
import jakarta.ws.rs.FormParam;

/**
 * Servicio REST para almacenar, actualizar y obtener documentos desde los
 * sistemas transversales y comunicarse con aplicación firmadigital-api
 *
 * @author Ricardo Arguello <ricardo.arguello@soportelibre.com>
 */
@Stateless
@Path("/documentos")
public class ServicioDocumentoRest {

    @EJB
    private ServicioDocumento servicioDocumento;

    @EJB
    private ServicioSistemaTransversal servicioSistemaTransversal;

    @EJB
    private ServicioLog servicioLog;

    private static final String API_KEY_HEADER_PARAMETER = "X-API-KEY";

    private static final Logger logger = Logger.getLogger(ServicioDocumentoRest.class.getName());

    /**
     * Almacena varios documentos desde un Sistema Transversal.
     *
     * Ejemplo:
     *
     * { "cedula":"12345678", "sistema":"quipux", "documentos":[
     * {"nombre":"Archivo1.pdf", "base64":"abc"} ] }
     *
     * @param apiKey
     * @param jsonParameter
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response crearDocumentos(@HeaderParam(API_KEY_HEADER_PARAMETER) String apiKey, String jsonParameter) {

        if (apiKey == null) {
            System.out.println("Se debe incluir un API Key!");
            return Response.status(Status.BAD_REQUEST).entity("Se debe incluir un API Key!").build();
        }

        if (jsonParameter == null || jsonParameter.isEmpty()) {
            System.out.println("Se debe incluir JSON!");
            return Response.status(Status.BAD_REQUEST).entity("Se debe incluir JSON!").build();
        }

        JsonReader jsonReader = Json.createReader(new StringReader(jsonParameter));
        JsonObject json;

        try {
            json = (JsonObject) jsonReader.read();
        } catch (JsonParsingException e) {
            System.out.println("::Error al decodificar JSON: \"" + e.getMessage() + "\"");
            return Response.status(Status.BAD_REQUEST).entity(getClass().getSimpleName() + "::Error al decodificar JSON: \"" + e.getMessage() + "\"")
                    .build();
        }

        String cedula;
        String sistema;

        try {
            cedula = json.getString("cedula");
        } catch (NullPointerException e) {
            System.out.println("::Error al decodificar JSON: Se debe incluir \"cedula\"");
            return Response.status(Status.BAD_REQUEST).entity(getClass().getSimpleName() + "::Error al decodificar JSON: Se debe incluir \"cedula\"")
                    .build();
        }

        try {
            sistema = json.getString("sistema");
        } catch (NullPointerException e) {
            System.out.println("::Error al decodificar JSON: Se debe incluir \"sistema\"");
            return Response.status(Status.BAD_REQUEST).entity(getClass().getSimpleName() + "::Error al decodificar JSON: Se debe incluir \"sistema\"")
                    .build();
        }

        // Verificar API KEY
        if (!servicioSistemaTransversal.verificarApiKey(sistema, apiKey)) {
            System.out.println("Error al validar API_KEY para el sistema: " + sistema);
            logger.log(Level.SEVERE, "Error al validar API_KEY para el sistema {0}", sistema);
            return Response.status(Status.FORBIDDEN).entity("Error al validar API_KEY").build();
        }

        JsonArray array = json.getJsonArray("documentos");

        if (array == null) {
            System.out.println("Error al decodificar JSON: Se debe incluir \"documentos\"");
            return Response.status(Status.BAD_REQUEST)
                    .entity("Error al decodificar JSON: Se debe incluir \"documentos\"").build();
        }

        // Documentos a devolver
        Map<String, String> documentos = new HashMap<>();

        for (JsonObject documentoJson : array.getValuesAs(JsonObject.class)) {
            String nombre = documentoJson.getString("nombre");
            String documento = documentoJson.getString("documento");
            documentos.put(nombre, documento);
        }

        try {
            // Crear un documento en el sistema, retorna un token JWT
            String token = servicioDocumento.crearDocumentos(cedula, sistema, documentos);

            // Retornar un token JWT
            return Response.status(Status.CREATED).entity(token).build();
        } catch (ServicioSistemaTransversalException e) {
            System.out.println("ServicioDocumentoRest::crearDocumentos ServicioSistemaTransversalException: " + e.getMessage());
            servicioLog.error("ServicioDocumentoRest::crearDocumentos", "ServicioSistemaTransversalException: " + e.getMessage());
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (Base64InvalidoException e) {
            System.out.println("ServicioDocumentoRest::crearDocumentos Error al decodificar Base64");
            servicioLog.error("ServicioDocumentoRest::crearDocumentos", "Error al decodificar Base64");
            return Response.status(Status.BAD_REQUEST).entity("Error al decodificar Base64").build();
        }
    }

    /**
     * Obtiene documentos mediante un token JWT.
     *
     * @param token
     * @return el documento en Base64
     */
    @GET
    @Path("{token}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response obtenerDocumentos(@PathParam("token") String token) {
        Map<Long, String> documentos;

        try {
            documentos = servicioDocumento.obtenerDocumentos(token);
        } catch (TokenInvalidoException e) {
            System.out.println("ServicioDocumentoRest::obtenerDocumentos Token invalido: " + token);
            servicioLog.error("ServicioDocumentoRest::obtenerDocumentos", "Token invalido: " + token);
            return Response.status(Status.BAD_REQUEST).entity("Token invalido").build();
        } catch (TokenExpiradoException e) {
            System.out.println("ServicioDocumentoRest::obtenerDocumentos Token expirado: " + token);
            servicioLog.error("ServicioDocumentoRest::obtenerDocumentos", "Token expirado: " + token);
            return Response.status(Status.BAD_REQUEST).entity("Token expirado").build();
        }

        JsonArrayBuilder array = Json.createArrayBuilder();

        for (Long id : documentos.keySet()) {
            String documento = documentos.get(id);
            array.add(Json.createObjectBuilder().add("id", id).add("documento", documento));
        }

//        System.out.println("array: "+array.build().toString());
//        System.out.println("array: "+array.build().size());
//        if (array.build().isEmpty()) {
//            return Response.status(Status.BAD_REQUEST).entity("Token gestionado").build();
//        }
        // La fecha actual en formato ISO-8601 (2017-08-27T17:54:43.562-05:00)
        String fechaHora = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        String json = Json.createObjectBuilder().add("fecha_hora", fechaHora).add("documentos", array).build()
                .toString();
        return Response.ok(json).build();
    }

    @PUT
    @Path("{token}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response actualizarDocumentos(@PathParam("token") String token, @FormParam("json") String json, @FormParam("base64") String base64) {
        JsonObject jsonObject;
        try (JsonReader jsonReader = Json.createReader(new StringReader(json))) {
            jsonObject = jsonReader.readObject();
        }

        String cedulaJson = jsonObject.getString("cedula");

        if (cedulaJson == null || cedulaJson.isEmpty()) {
            System.out.println("ServicioDocumentoRest::actualizarDocumentos Cedula vacia");
            servicioLog.error("ServicioDocumentoRest::actualizarDocumentos", "Cedula vacia");
            return Response.status(Status.BAD_REQUEST).entity("Cedula vacia").build();
        }

        List<JsonObject> array = jsonObject.getJsonArray("documentos").getValuesAs(JsonObject.class);

        if (array.size() == 0) {
            System.out.println("ServicioDocumentoRest::actualizarDocumentos No se encuentran documentos");
            servicioLog.error("ServicioDocumentoRest::actualizarDocumentos", "No se encuentran documentos");
            return Response.status(Status.BAD_REQUEST).entity("No se encuentran documentos").build();
        }

        Map<Long, String> documentos = new HashMap<>();

        for (JsonObject documentoJson : array) {
            Integer id;

            try {
                id = documentoJson.getInt("id");
            } catch (NullPointerException e) {
                System.out.println("ServicioDocumentoRest::actualizarDocumentos No se encuentra id");
                servicioLog.error("ServicioDocumentoRest::actualizarDocumentos", "No se encuentra id");
                return Response.status(Status.BAD_REQUEST).entity("No se encuentra id").build();
            } catch (ClassCastException e) {
                System.out.println("ServicioDocumentoRest::actualizarDocumentos id no es un int");
                servicioLog.error("ServicioDocumentoRest::actualizarDocumentos", "id no es un int");
                return Response.status(Status.BAD_REQUEST).entity("No se encuentra id").build();
            }

            String documento = documentoJson.getString("documento");

            if (documento == null || documento.isEmpty()) {
                servicioLog.error("ServicioDocumentoRest::actualizarDocumentos", "No se encuentra documento");
                return Response.status(Status.BAD_REQUEST).entity("No se encuentra documento").build();
            }

            documentos.put(id.longValue(), documento);
        }

        try {
            int documentosFirmados = servicioDocumento.actualizarDocumentos(token, documentos, cedulaJson, base64);
            JsonObject jsonResponse = Json.createObjectBuilder().add("documentos_recibidos", documentos.size())
                    .add("documentos_firmados", documentosFirmados).build();
            return Response.ok(jsonResponse).build();
        } catch (CedulaInvalidaException e) {
            System.out.println("ServicioDocumentoRest::actualizarDocumentos Cedula invalida");
            servicioLog.error("ServicioDocumentoRest::actualizarDocumentos", "Cedula invalida: " + e.getMessage());
            return generarErrorResponse("Cedula invalida");
        } catch (CertificadoRevocadoException e) {
            System.out.println("ServicioDocumentoRest::actualizarDocumentos Certificado revocado");
            servicioLog.error("ServicioDocumentoRest::actualizarDocumentos", e.getMessage());
            return generarErrorResponse("Certificado revocado");
        } catch (ServicioSistemaTransversalException e) {
            System.out.println("ServicioDocumentoRest::actualizarDocumentos No se encontraron documentos para firmar");
            servicioLog.error("ServicioDocumentoRest::actualizarDocumentos", e.getMessage());
            return generarErrorResponse("No se encontraron documentos para firmar");
        } catch (TokenInvalidoException e) {
            System.out.println("ServicioDocumentoRest::actualizarDocumentos Token invalido: " + token);
            servicioLog.error("ServicioDocumentoRest::actualizarDocumentos", "Token invalido: " + token);
            return generarErrorResponse("Token inválido");
        } catch (TokenExpiradoException e) {
            System.out.println("ServicioDocumentoRest::actualizarDocumentos Token expirado: " + token);
            servicioLog.error("ServicioDocumentoRest::actualizarDocumentos", "Token expirado: " + token);
            return generarErrorResponse("Token expirado");
        } catch (Base64InvalidoException e) {
            System.out.println("ServicioDocumentoRest::actualizarDocumentos Base 64 invalido");
            servicioLog.error("ServicioDocumentoRest::actualizarDocumentos", "Base 64 invalido");
            return generarErrorResponse("Base 64 inválido");
        } catch (DocumentoNoExisteException e) {
            System.out.println("ServicioDocumentoRest::actualizarDocumentos No existe documento");
            servicioLog.error("ServicioDocumentoRest::actualizarDocumentos", "No existe documento");
            return generarErrorResponse("No existe documento");
        }
    }

    private Response generarErrorResponse(String error) {
        JsonObject errorResponse = Json.createObjectBuilder().add("error", error).build();
        return Response.status(Status.NOT_ACCEPTABLE).type(MediaType.TEXT_PLAIN).entity(errorResponse).build();
    }
}
