package example.pdfbox;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.COSObjectable;
import org.apache.pdfbox.pdmodel.common.PDNumberTreeNode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDObjectReference;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.StandardStructureTypes;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkInfo;

import static org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA;

public class TaggedSignatureFieldPDFBox2 {
    public static void main(String[] args)
    {
        try (PDDocument doc = new PDDocument())
        {
            // 1) Mark as tagged
            PDDocumentCatalog catalog = doc.getDocumentCatalog();
            PDMarkInfo markInfo = new PDMarkInfo();
            markInfo.setMarked(true);
            catalog.setMarkInfo(markInfo);
            catalog.setLanguage("en-US"); // optional

            // 2) Create the structure tree root
            PDStructureTreeRoot structureTreeRoot = new PDStructureTreeRoot();
            catalog.setStructureTreeRoot(structureTreeRoot);

            // 3) Create /Document and /Form elements
            PDStructureElement docElement =
                    new PDStructureElement(StandardStructureTypes.DOCUMENT, structureTreeRoot);
            PDStructureElement formElement =
                    new PDStructureElement(StandardStructureTypes.FORM, docElement);

            // 4) Add a page
            PDPage page = new PDPage();
            doc.addPage(page);

            // (Optional) draw some text
            try (PDPageContentStream cs = new PDPageContentStream(doc, page))
            {
                cs.beginText();
                cs.setFont(new PDType1Font(HELVETICA), 12);
                cs.setLeading(14.5f);
                cs.newLineAtOffset(100, 700);
                cs.showText("Example page with a signature field below.");
                cs.endText();
            }

            // 5) Create an AcroForm + signature field
            PDAcroForm acroForm = new PDAcroForm(doc);
            catalog.setAcroForm(acroForm);

            PDSignatureField signatureField = new PDSignatureField(acroForm);
            signatureField.setPartialName("SignatureField1");

            // 6) Configure the widget annotation
            PDAnnotationWidget widget = signatureField.getWidgets().get(0);
            widget.setRectangle(new PDRectangle(100, 600, 200, 50));
            widget.setPage(page);

            page.getAnnotations().add(widget);
            acroForm.getFields().add(signatureField);

            // 7) **Manually** link the annotation to the /Form structure element
            addAnnotationToStructTree(widget, formElement, structureTreeRoot);

            // 8) Save
            doc.save("~//Downloads/TaggedSignatureField3.pdf");

            doc.save("TaggedSignatureField-Manual3.pdf");
            System.out.println("PDF saved. Test in PAC 3 for 'Widget annotation not nested' error resolution.");

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Manually attach an annotation to the structure tree under a given parent element.
     * Creates an /OBJR kid, sets /StructParent on the annotation, and updates the parent tree.
     */
    private static void addAnnotationToStructTree(
            PDAnnotationWidget annotation,
            PDStructureElement parentElement,
            PDStructureTreeRoot structureTreeRoot
    ) throws IOException
    {
        // (A) Get the next parent-tree index
        int structParent = structureTreeRoot.getParentTreeNextKey();

        // (B) Set /StructParent on the annotation
        annotation.getCOSObject().setInt(COSName.STRUCT_PARENT, structParent);

        // (C) Build the /OBJR dictionary referencing the annotation
        COSDictionary objrDict = new COSDictionary();
        objrDict.setItem(COSName.TYPE, COSName.OBJR);
        objrDict.setItem(COSName.OBJ, annotation.getCOSObject());

        PDObjectReference objRef = new PDObjectReference(objrDict);

        // (D) Append the /OBJR as a kid of the parentElement (here, /Form)
        parentElement.appendKid(objRef);

        // (E) Update the parent tree (structParent -> OBJR)
        PDNumberTreeNode parentTree = structureTreeRoot.getParentTree();
        if (parentTree == null)
        {
            parentTree = new PDNumberTreeNode(new COSDictionary(), null);
            structureTreeRoot.setParentTree(parentTree);
        }

        // Retrieve or create the existing map of structParent -> COS object(s)
        Map<Integer, COSObjectable> numbers = parentTree.getNumbers();
        if (numbers == null)
        {
            numbers = new HashMap<>();
        }

        // Usually we store references in a COSArray in case multiple references share the same structParent
        COSArray kidArray = new COSArray();
        kidArray.add(objrDict);

        numbers.put(structParent, kidArray);
        parentTree.setNumbers(numbers);

        // (F) Increment the next key for the next annotation
        structureTreeRoot.setParentTreeNextKey(structParent + 1);
    }
}
