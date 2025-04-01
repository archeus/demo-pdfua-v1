package example.pdfbox;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkInfo;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;

import java.io.File;

public class SignatureFieldExample {
    public static void main(String[] args) throws Exception {
        try (PDDocument document = new PDDocument()) {
//            PDPage page = document.getPage(0);
            PDPage page = new PDPage();
            document.addPage(page);


            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            if (acroForm == null) {
                acroForm = new PDAcroForm(document);
                document.getDocumentCatalog().setAcroForm(acroForm);
            }

            // Create signature field
            PDSignatureField signatureField = new PDSignatureField(acroForm);
            signatureField.setPartialName("Signature1");

            // Create widget annotation for signature field
            PDAnnotationWidget widget = signatureField.getWidgets().get(0);
            PDRectangle rect = new PDRectangle(50, 600, 200, 50);
            widget.setRectangle(rect);
            widget.setPage(page);

            // Optional: Add border and style
            PDBorderStyleDictionary border = new PDBorderStyleDictionary();
            border.setWidth(1);
            widget.setBorderStyle(border);

            page.getAnnotations().add(widget);
            acroForm.getFields().add(signatureField);


//                COSDictionary markInfo = new COSDictionary();
//                markInfo.setBoolean(COSName.MARKED, true);
//                catalogDict.setItem(COSName.MARK_INFO, markInfo);
                PDMarkInfo markInfo = new PDMarkInfo();
                markInfo.setMarked(true);
                document.getDocumentCatalog().setMarkInfo(markInfo);

            document.save("output.pdf");
        }
    }
}
