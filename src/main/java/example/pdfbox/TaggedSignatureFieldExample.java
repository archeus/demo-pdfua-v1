package example.pdfbox;

import java.io.IOException;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDObjectReference;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkInfo;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.StandardStructureTypes;
//import org.apache.pdfbox.pdmodel.font.PDType1Font;

import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;

import static org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA;

//import static org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA;

public class TaggedSignatureFieldExample
{
    public static void main(String[] args)
    {
        try (PDDocument doc = new PDDocument())
        {
            // 1) Mark the PDF as "tagged"
            PDDocumentCatalog catalog = doc.getDocumentCatalog();
            PDMarkInfo markInfo = new PDMarkInfo();
            markInfo.setMarked(true);
            catalog.setMarkInfo(markInfo);

            // (Optional) Set the document language
            catalog.setLanguage("en-US");

            // 2) Create/Set the Structure Tree Root
            PDStructureTreeRoot structureTreeRoot = new PDStructureTreeRoot();
            catalog.setStructureTreeRoot(structureTreeRoot);

            // 3) Create a top-level /Document structure element
            PDStructureElement docElement =
                    new PDStructureElement(StandardStructureTypes.DOCUMENT, structureTreeRoot);

            // 4) Create a /Form structure element under /Document
            PDStructureElement formElement =
                    new PDStructureElement(StandardStructureTypes.FORM, docElement);

            // 5) Add a page to the document
            PDPage page = new PDPage();
            doc.addPage(page);

            // (Optional) Draw some text for demonstration
            try (PDPageContentStream cs = new PDPageContentStream(doc, page))
            {
                cs.beginText();
                cs.setFont(new PDType1Font(HELVETICA), 12);
                cs.setLeading(14.5f);
                cs.newLineAtOffset(100, 700);
                cs.showText("Example page with a signature field below.");
                cs.endText();
            }

            // 6) Create an AcroForm if not already present
            PDAcroForm acroForm = new PDAcroForm(doc);
            catalog.setAcroForm(acroForm);

            // 7) Create a signature field
            PDSignatureField signatureField = new PDSignatureField(acroForm);
            signatureField.setPartialName("SignatureField1");

            // 8) Configure the widget annotation
            PDAnnotationWidget widget = signatureField.getWidgets().get(0);
            widget.setRectangle(new PDRectangle(100, 600, 200, 50));  // x, y, width, height
            widget.setPage(page);

            // Add the widget to the page's annotations
            page.getAnnotations().add(widget);
            // Add the field to the AcroForm
            acroForm.getFields().add(signatureField);

            // 9) Create an OBJR reference for the widget annotation
            COSDictionary objrDict = new COSDictionary();
            objrDict.setItem(COSName.TYPE, COSName.OBJR);       // /Type /OBJR
            objrDict.setItem(COSName.OBJ, widget.getCOSObject()); // /OBJ -> the widget's COS object

            PDObjectReference objRef = new PDObjectReference(objrDict);

            // 10) Append the object reference under the /Form element
            formElement.appendKid(objRef);

            // 11) Save the document
            doc.save("~//Downloads/TaggedSignatureField.pdf");
            doc.save("TaggedSignatureField.pdf");

            System.out.println("PDF saved as TaggedSignatureField.pdf");
            System.out.println("Open it in a PDF/UA checker (like PAC 3) to verify accessibility.");
        }
        catch (IOException e)
        {
            e.printStackTrace();
            throw new RuntimeException("Error creating tagged PDF", e);
        }
    }
}
