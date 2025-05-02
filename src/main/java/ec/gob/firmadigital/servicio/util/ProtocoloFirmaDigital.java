/*
 * Copyright (C) 2024 
 * Authors: Misael Fernández
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import ec.gob.firmadigital.servicio.exception.ProtocoloInvalidoException;

public class ProtocoloFirmaDigital {
    private final String SISTEMA;
    private final String OPERACION;
    private final Map<String, String> PARAMETROS;
    private static final String PROTOCOLO = "firmaec";

    public ProtocoloFirmaDigital(String str) throws ProtocoloInvalidoException {
        URI uri;
        try {
            uri = new URI(str);
        } catch (URISyntaxException e) {
            throw new ProtocoloInvalidoException(e);
        }
        if (!PROTOCOLO.equals(uri.getScheme())) {
            throw new ProtocoloInvalidoException("Solo se soporta el protocolo '" + PROTOCOLO + "'");
        }
        if (uri.getAuthority() == null || uri.getQuery() == null || uri.getQuery().isEmpty()) {
            throw new ProtocoloInvalidoException("Se debe incluir un sistema en el protocolo");
        }
        if (uri.getPath() == null || uri.getQuery() == null || uri.getQuery().isEmpty()) {
            throw new ProtocoloInvalidoException("Se debe incluir una operacion en el protocolo");
        }
        if (uri.getQuery() == null || uri.getQuery() == null || uri.getQuery().isEmpty()) {
            throw new ProtocoloInvalidoException("Se deben incluir parámetros en el protocolo");
        }
        this.SISTEMA = uri.getAuthority();
        this.OPERACION = uri.getPath();
        this.PARAMETROS = parseQuery(uri.getQuery());
    }

    public String getSistema() {
        return SISTEMA;
    }

    public String getOperacion() {
        return OPERACION;
    }

    public Map<String, String> getParametros() {
        return PARAMETROS;
    }

    /**
     * Analiza el query y extrae un Map<String, String> con los parametros y sus
     * valores.
     */
    private Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        String[] parameters = query.split("&");
        for (String param : parameters) {
            String[] valores = param.split("=");
            String name = valores[0];
            String value = (valores.length == 2 ? valores[1] : null);
            map.put(name, value);
        }
        return map;
    }
}
