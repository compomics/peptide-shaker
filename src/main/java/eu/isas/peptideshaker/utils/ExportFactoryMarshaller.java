package eu.isas.peptideshaker.utils;

import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.io.json.JsonMarshaller;

/**
 * This class is a convenience class to have a DefaultJsonConverter with the
 * ExportFactory interfaces.
 *
 * @author Harald Barsnes
 */
public class ExportFactoryMarshaller extends JsonMarshaller {

    /**
     * Constructor.
     */
    public ExportFactoryMarshaller() {
        super(ExportFeature.class);
    }
}
