/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2019, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.geojson.feature;

import org.geotools.geojson.GeoJSONUtil;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class FeatureTypeAttributeIO implements AttributeIO {

    HashMap<String, AttributeIO> ios = new HashMap<>();

    public FeatureTypeAttributeIO(SimpleFeatureType featureType) {
        for (AttributeDescriptor ad : featureType.getAttributeDescriptors()) {
            AttributeIO io = null;
            if (Date.class.isAssignableFrom(ad.getType().getBinding())) {
                io = new DateAttributeIO();
            } else {
                io = new DefaultAttributeIO();
            }
            ios.put(ad.getLocalName(), io);
        }
    }

    @Override
    public String encode(String att, Object value) {
        return ios.get(att).encode(att, value);
    }

    @Override
    public Object parse(String att, String value) {
        return ios.get(att).parse(att, value);
    }

    static class DateAttributeIO implements AttributeIO {

        @Override
        public String encode(String att, Object value) {
            return GeoJSONUtil.dateFormatter.format((Date) value);
        }

        @Override
        public Object parse(String att, String value) {
            try {
                final SimpleDateFormat sdf = new SimpleDateFormat(GeoJSONUtil.DATE_FORMAT);
                return sdf.parse(value);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
