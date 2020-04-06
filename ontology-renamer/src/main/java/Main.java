import org.apache.commons.io.IOUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.OWLEntityRenamer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;



public class Main {
    public static void main(String[] args) throws OWLOntologyCreationException, OWLOntologyStorageException, FileNotFoundException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Place the .owl file in resources directory and enter file name");
        System.out.print("Enter file name:  ");
        String fileName = scanner.nextLine();
        String directoryPath = System.getProperty("user.dir") + "/src/main/resources/";

        if(fileName.length() < 5 || fileName.substring(fileName.length() - 4) != ".owl")
            fileName += ".owl";

        File ontologyFile = new File(directoryPath + fileName);

        if(!ontologyFile.exists())
            throw new FileNotFoundException("File: " + ontologyFile.getAbsolutePath() + " not found");

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(ontologyFile);
        OWLEntityRenamer owlEntityRenamer = new OWLEntityRenamer(manager,	manager.getOntologies());

        List<OWLClass> classes = new ArrayList<>(ontology.getClassesInSignature());

        for(OWLClass owlClass : classes) {
            List<OWLAnnotationAssertionAxiom> annotationList = ontology.annotationAssertionAxioms(owlClass.getIRI()).collect(Collectors.toList());

            if (annotationList.size() != 1) {
                System.out.println(" === Wrong annotations number: " + Integer.toString(annotationList.size()) + " annotations found: " + owlClass.toString() + " ===");
                continue;
            }

            Optional<OWLLiteral> literalOpt = annotationList.get(0).getValue().asLiteral();

            if (!literalOpt.isPresent())
                throw new RuntimeException("literalOpt not present for: " + annotationList.toString());

            String label = literalOpt.get().getLiteral();
            int prefixIdx = owlClass.getIRI().toString().lastIndexOf('/');
            String newIRI = owlClass.getIRI().toString().substring(0, prefixIdx) + "/" + label;

            manager.applyChanges(owlEntityRenamer.changeIRI(owlClass.getIRI(), IRI.create(newIRI)));
        }

        OutputStream outputFile = new FileOutputStream(directoryPath + "new_" + fileName);
        try {
            manager.saveOntology(ontology, outputFile);
            System.out.println("All changes saved in original file: new_" + fileName);
        }
        finally {
            IOUtils.closeQuietly(outputFile);
        }
    }
}