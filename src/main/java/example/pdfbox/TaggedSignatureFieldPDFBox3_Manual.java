package example.pdfbox;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
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

public class TaggedSignatureFieldPDFBox3_Manual
{
    public static void main(String[] args)
    {
        try (PDDocument doc = new PDDocument())
        {
            // 1) Enable tagging
            PDDocumentCatalog catalog = doc.getDocumentCatalog();
            PDMarkInfo markInfo = new PDMarkInfo();
            markInfo.setMarked(true);
            catalog.setMarkInfo(markInfo);
            catalog.setLanguage("en-US"); // optional but recommended

            // 2) Create the Structure Tree Root
            PDStructureTreeRoot structureTreeRoot = new PDStructureTreeRoot();
            catalog.setStructureTreeRoot(structureTreeRoot);

            // 3) Create /Document and /Form elements
            PDStructureElement docElement = new PDStructureElement(
                    StandardStructureTypes.DOCUMENT, structureTreeRoot);
            PDStructureElement formElement = new PDStructureElement(
                    StandardStructureTypes.FORM, docElement);

            // 4) Add a page
            PDPage page = new PDPage();
            doc.addPage(page);

            // Optional text for demonstration
            try (PDPageContentStream cs = new PDPageContentStream(doc, page))
            {
                cs.beginText();
                cs.setFont(new PDType1Font(HELVETICA), 12);
                cs.setLeading(14.5f);
                cs.newLineAtOffset(100, 700);
                cs.showText("Example page with a signature field below.");
                cs.endText();
            }

            // 5) Create the AcroForm + signature field
            PDAcroForm acroForm = new PDAcroForm(doc);
            catalog.setAcroForm(acroForm);

            PDSignatureField signatureField = new PDSignatureField(acroForm);
            signatureField.setPartialName("MySignatureField");

            // 6) Configure the widget annotation
            PDAnnotationWidget widget = signatureField.getWidgets().get(0);
            widget.setRectangle(new PDRectangle(100, 600, 200, 50));
            widget.setPage(page);

            page.getAnnotations().add(widget);
            acroForm.getFields().add(signatureField);

            // 7) The critical part: manually link the annotation into the structure.
            addAnnotationToStructTree(widget, formElement, structureTreeRoot);

            // 8) Save
            doc.save("~//Downloads/TaggedSignatureField5.pdf");

            doc.save("TaggedSignatureField-Manual5.pdf");
            System.out.println("Saved TaggedSignatureField-Manual.pdf.");
            System.out.println("Open it in PAC 3 to confirm the widget is recognized inside /Form.");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Manually attach an annotation to the structure tree under a given parent element.
     * 1) Assign a unique /StructParent,
     * 2) Create an /OBJR dictionary and append it as a kid of /Form (or any parent),
     * 3) Update the Parent Tree so the annotation is truly recognized as nested.
     */
    private static void addAnnotationToStructTree(
            PDAnnotationWidget annotation,
            PDStructureElement parentElement,      // e.g. your /Form element
            PDStructureTreeRoot structureTreeRoot
    ) throws IOException
    {
        // 1) Get a valid parent‐tree index
        int structParent = structureTreeRoot.getParentTreeNextKey();
        if (structParent < 0) {
            structParent = 0;  // initialize if needed
            structureTreeRoot.setParentTreeNextKey(structParent);
        }

        // 2) Assign /StructParent to the annotation
        annotation.getCOSObject().setInt(COSName.STRUCT_PARENT, structParent);

        // 3) Create the /OBJR reference dictionary
        COSDictionary objrDict = new COSDictionary();
        objrDict.setItem(COSName.TYPE, COSName.OBJR);
        objrDict.setItem(COSName.OBJ, annotation.getCOSObject());

        // Wrap it in PDObjectReference
        PDObjectReference objRef = new PDObjectReference(objrDict);

        // 4) Append /OBJR to the /Form element
        parentElement.appendKid(objRef);

        // 5) Update the parent tree
        PDNumberTreeNode parentTree = structureTreeRoot.getParentTree();
        if (parentTree == null) {
            // PDFBox 3.x requires a COSDictionary & a “value type” (null = raw COSBase)
            parentTree = new PDNumberTreeNode(new COSDictionary(), null);
            structureTreeRoot.setParentTree(parentTree);
        }
        Map<Integer, COSObjectable> numbers = parentTree.getNumbers();
        if (numbers == null) {
            numbers = new HashMap<>();
        }

        // We store references in a COSArray
        COSArray kidArray = new COSArray();
        kidArray.add(objrDict);

        numbers.put(structParent, kidArray);
        parentTree.setNumbers(numbers);

        // 6) Increment so the next annotation gets a new ID
        structureTreeRoot.setParentTreeNextKey(structParent + 1);
    }

}
