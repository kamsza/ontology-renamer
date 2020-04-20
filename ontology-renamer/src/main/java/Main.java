import org.apache.commons.io.IOUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
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

        if(fileName.length() < 5 || !fileName.substring(fileName.length() - 4).equals(".owl"))
            fileName += ".owl";

        File ontologyFile = new File(directoryPath + fileName);

        if(!ontologyFile.exists())
            throw new FileNotFoundException("File: " + ontologyFile.getAbsolutePath() + " not found");

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(ontologyFile);
        OWLEntityRenamer owlEntityRenamer = new OWLEntityRenamer(manager, manager.getOntologies());

        List<OWLClass> classes = new ArrayList<>(ontology.getClassesInSignature());
        List<OWLDataProperty> dataProperties = new ArrayList<>(ontology.getDataPropertiesInSignature());
        List<OWLObjectProperty> objectProperties = new ArrayList<>(ontology.getObjectPropertiesInSignature());

        List<OWLNamedObject> ontologyObjects = new ArrayList<>();
        ontologyObjects.addAll(classes);
        ontologyObjects.addAll(dataProperties);
        ontologyObjects.addAll(objectProperties);

        System.out.println("_____ CHANGES MADE _____");
        for(OWLNamedObject owlObject : ontologyObjects) {
            List<OWLAnnotationAssertionAxiom> annotationList = ontology.annotationAssertionAxioms(owlObject.getIRI()).collect(Collectors.toList());

            if (annotationList.size() != 1) {
                System.out.println(" === Wrong annotations number: " + Integer.toString(annotationList.size()) + " annotations found: " + owlObject.toString() + " ===\n");
                continue;
            }

            Optional<OWLLiteral> literalOpt = annotationList.get(0).getValue().asLiteral();

            if (!literalOpt.isPresent())
                throw new RuntimeException("literalOpt not present for: " + annotationList.toString());

            String label = literalOpt.get().getLiteral();
            int prefixIdx = owlObject.getIRI().toString().lastIndexOf('/');
            String newIRI = owlObject.getIRI().toString().substring(0, prefixIdx) + "/" + label;

            System.out.println("- " + owlObject.getIRI().toString());
            System.out.println("+ " + newIRI + "\n");

            manager.applyChanges(owlEntityRenamer.changeIRI(owlObject.getIRI(), IRI.create(newIRI)));
        }

        OutputStream outputFile = new FileOutputStream(directoryPath + "changed_" + fileName);
        try {
            manager.saveOntology(ontology, outputFile);
            System.out.println("All changes saved in original file: changed_" + fileName);
        }
        finally {
            IOUtils.closeQuietly(outputFile);
        }
    }
}
