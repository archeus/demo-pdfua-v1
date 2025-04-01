package example.pdfbox;

import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkInfo;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class FixPdfUaStructureV1 {
    public static void main(String[] args) throws IOException {
        String src = "pdfua_signature_fixed_ko.pdf";     // Your original file with a signature field
        String dest = "output_fixed.pdf";

        Path inputPath = Path.of(src);
//        File output = new File("template_with_signature.pdf");
//
        try (RandomAccessRead rar = new RandomAccessReadBufferedFile(inputPath)) {
//
            PDFParser parser = new PDFParser(rar);
//
            try (PDDocument doc = parser.parse()) {

//        try (PDDocument doc = PDDocument.load(new File(src))) {
                PDDocumentCatalog catalog = doc.getDocumentCatalog();

                PDMarkInfo markInfo = new PDMarkInfo();
                markInfo.setMarked(true);
                catalog.setMarkInfo(markInfo);


                PDAcroForm acroForm = catalog.getAcroForm();

                if (acroForm == null) throw new RuntimeException("No AcroForm present");

                // Get the signature field and its widget
                PDSignatureField sigField = (PDSignatureField) acroForm.getField("Signature1");
                PDAnnotationWidget widget = sigField.getWidgets().get(0);

                // Assign a StructParent to the widget (required)
                COSDictionary widgetDict = widget.getCOSObject();
                int structParent = 0; // This should match index in ParentTree
                widgetDict.setInt("StructParent", structParent);

                // Build the StructTreeRoot
                COSDictionary structTreeRoot = new COSDictionary();
                structTreeRoot.setName("Type", "StructTreeRoot");
                structTreeRoot.setItem(COSName.TYPE, COSName.getPDFName("StructTreeRoot"));

                // Create the /Form StructElem
                COSDictionary formElem = new COSDictionary();
                formElem.setItem(COSName.TYPE, COSName.getPDFName("StructElem"));
                formElem.setItem(COSName.S, COSName.getPDFName("Form")); // Role: Form
                formElem.setItem(COSName.P, structTreeRoot); // Parent = StructTreeRoot

                // Create an OBJR that references the annotation
                COSDictionary objr = new COSDictionary();
                objr.setItem(COSName.TYPE, COSName.getPDFName("OBJR"));
                objr.setItem(COSName.OBJ, widgetDict);
                formElem.setItem(COSName.K, objr); // Add to children of <Form>


                // Set /K (kids) of StructTreeRoot to be our Form element
                structTreeRoot.setItem(COSName.K, formElem);

                // Create ParentTree that maps structParent -> formElem
                COSDictionary parentTree = new COSDictionary();
                COSArray nums = new COSArray();
                nums.add(COSInteger.get(structParent));
                nums.add(formElem);
                parentTree.setItem(COSName.NUMS, nums);
                structTreeRoot.setItem(COSName.PARENT_TREE, parentTree);
                structTreeRoot.setInt("ParentTreeNextKey", structParent + 1);

                // Set Lang (optional but recommended)
                catalog.setLanguage("en-US");

                // Inject into the catalog
                COSDictionary catalogDict = catalog.getCOSObject();
                catalogDict.setItem(COSName.STRUCT_TREE_ROOT, structTreeRoot);

                COSDictionary roleMap = new COSDictionary();
                roleMap.setItem(COSName.getPDFName("Form"), COSName.getPDFName("Form"));
                structTreeRoot.setItem(COSName.ROLE_MAP, roleMap);

//                COSObject structTreeRef = doc.getDocument().createCOSObject(structTreeRoot);
//                catalogDict.setItem(COSName.STRUCT_TREE_ROOT, structTreeRef);

                doc.save(dest);
                System.out.println("Fixed PDF/UA structure saved to: " + dest);
            }
        }
    }
}
