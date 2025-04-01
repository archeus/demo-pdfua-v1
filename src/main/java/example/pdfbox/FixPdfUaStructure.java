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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

//https://chatgpt.com/share/67eb920c-cd60-8004-9f7f-ae50e3dc922f
public class FixPdfUaStructure {
    public static void main(String[] args) throws IOException {
//        String src = "pdfua_signature_fixed_ko.pdf";     // Your original file with a signature field
//        String src = "pdfua_signature_to_fix.pdf";     // Your original file with a signature field
        String src = "to_fix.pdf";     // Your original file with a signature field
        Path inputPath = Path.of(src);
//        String dest = "output_fixed.pdf";
        String dest = "/Users/ionutpaduraru/Downloads/output_fixed.pdf";
        new File(dest).delete();

        try (RandomAccessRead rar = new RandomAccessReadBufferedFile(inputPath)) {
            PDFParser parser = new PDFParser(rar);
            try (PDDocument doc = parser.parse()) {
                PDDocumentCatalog catalog = doc.getDocumentCatalog();
                COSDictionary catalogDict = catalog.getCOSObject();

                PDAcroForm acroForm = catalog.getAcroForm();
                if (acroForm == null) throw new RuntimeException("No AcroForm present");

                // -- Step 1: Get the signature field and its widget
                PDSignatureField sigField = (PDSignatureField) acroForm.getField("Signature1");
                if (sigField == null) throw new RuntimeException("Signature field 'Signature1' not found");

                List<PDAnnotationWidget> widgets = sigField.getWidgets();
                if (widgets.isEmpty()) throw new RuntimeException("No widget found for the signature field");

                PDAnnotationWidget widget = widgets.get(0);
                COSDictionary widgetDict = widget.getCOSObject();

                // -- Step 2: Assign StructParent to the widget
                int structParent = 0; // This index must match the ParentTree mapping
                widgetDict.setInt("StructParent", structParent);

                // -- Step 3: Build the StructElem of role /Form
                COSDictionary formElem = new COSDictionary();
                formElem.setItem(COSName.TYPE, COSName.getPDFName("StructElem"));
                formElem.setItem(COSName.S, COSName.getPDFName("Form")); // Role
                COSDictionary structTreeRoot = new COSDictionary(); // Placeholder, will be set below
                formElem.setItem(COSName.P, structTreeRoot); // Parent (to be updated)

                // Create /OBJR referencing the widget
                COSDictionary objr = new COSDictionary();
                objr.setItem(COSName.TYPE, COSName.getPDFName("OBJR"));
                objr.setItem(COSName.OBJ, widgetDict);
                formElem.setItem(COSName.K, objr); // Kids = annotation reference

                // -- Step 4: Build ParentTree and Nums array
                COSDictionary parentTree = new COSDictionary();
                COSArray nums = new COSArray();
                nums.add(COSInteger.get(structParent)); // key = StructParent
                nums.add(formElem);                    // value = Form StructElem
                parentTree.setItem(COSName.NUMS, nums);

                // -- Step 5: Build StructTreeRoot and complete references
                structTreeRoot.setItem(COSName.TYPE, COSName.getPDFName("StructTreeRoot"));
                structTreeRoot.setItem(COSName.K, formElem); // top-level element
                structTreeRoot.setItem(COSName.PARENT_TREE, parentTree);
                structTreeRoot.setInt("ParentTreeNextKey", structParent + 1);

                // Add RoleMap (important for PAC)
                COSDictionary roleMap = new COSDictionary();
                roleMap.setItem(COSName.getPDFName("Form"), COSName.getPDFName("Form"));
                structTreeRoot.setItem(COSName.ROLE_MAP, roleMap);

                // -- Step 6: Set StructTreeRoot in catalog
                catalogDict.setItem(COSName.STRUCT_TREE_ROOT, structTreeRoot);

                // -- Step 7: Add required PDF/UA metadata
                catalog.setLanguage("en-US");

//                COSDictionary markInfo = new COSDictionary();
//                markInfo.setBoolean(COSName.MARKED, true);
//                catalogDict.setItem(COSName.MARK_INFO, markInfo);
                PDMarkInfo markInfo = new PDMarkInfo();
                markInfo.setMarked(true);
                catalog.setMarkInfo(markInfo);

                // Save the updated file
                doc.save(dest);
                System.out.println("✅ PDF fixed and saved to: " + dest);
            }
        }
    }
}
