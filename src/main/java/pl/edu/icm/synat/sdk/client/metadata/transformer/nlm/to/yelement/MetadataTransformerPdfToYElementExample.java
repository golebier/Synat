package pl.edu.icm.synat.sdk.client.metadata.transformer.nlm.to.yelement;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jdom.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import pl.edu.icm.cermine.PdfNLMContentExtractor;
import pl.edu.icm.cermine.exception.AnalysisException;
import pl.edu.icm.model.bwmeta.y.YExportable;
import pl.edu.icm.model.transformers.MetadataWriter;
import pl.edu.icm.model.transformers.bwmeta.desklight.LegacyBwmetaTransformers;
import pl.edu.icm.model.transformers.bwmeta.y.BwmetaTransformerConstants;
import pl.edu.icm.synat.api.services.SynatServiceRef;
import pl.edu.icm.synat.api.services.store.StatelessStore;
import pl.edu.icm.synat.cermine.pdf.to.nlm.CerminePdf2NlmString;
import pl.edu.icm.synat.cermine.transform.impl.CermineTransformerImpl;

/**
 * {@code MetadataTransformerPdfToYElementExample} example.
*
* @author Gra <Gołębiewski Radosław A.> google.com/+RadoslawGolebiewski
*
*/
@Component
public class MetadataTransformerPdfToYElementExample {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataTransformerPdfToYElementExample.class);
    private static final String PDF_FILE = "/home/Rt/Data/501423f0-0b37-3794-9e67-35b8abf1edf0.pdf";
    private static final String BWMETA_FILE = "/home/Rt/Data/501423f0-0b37-3794-9e67-35b8abf1edf0.bwmeta";

    @SynatServiceRef(serviceId = "Store")
    private StatelessStore store;
    
    public static void main(final String[] args) throws IOException, JDOMException, AnalysisException, SAXException {
        final File file = new File(PDF_FILE);
        if (null != file && file.exists()) {
            CermineTransformerImpl transformer = new CermineTransformerImpl(
                    new CerminePdf2NlmString(new PdfNLMContentExtractor()));
            final List<YExportable> yExportables = transformer.transform(new FileInputStream(file));
            MetadataWriter<YExportable> metadataWriter = LegacyBwmetaTransformers.BTF.getWriter(BwmetaTransformerConstants.Y, BwmetaTransformerConstants.BWMETA_2_1);
            final String bwmeta = metadataWriter.write(yExportables);
            LOGGER.info(bwmeta);
            final File bwmetaFile = new File(BWMETA_FILE);
            FileUtils.write(bwmetaFile, bwmeta);
        }
    }
}
