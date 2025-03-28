package organizer;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import java.nio.file.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.io.IOException;

import organizer.rule.*;


public class NeatFileApp extends Application {
    private NeatFileLogic organizer = new NeatFileLogic();
    private Group currentGroup;
    private Set<Path> watchDirs = new HashSet<>(Arrays.asList(
        Paths.get("watch1"), 
        Paths.get("watch2")
    ));

    private Thread watchServiceThread;
    private volatile boolean running = true;

    @Override
    public void start(Stage primaryStage) {
        Pane root = new Pane();
        Scene scene = new Scene(root, 800, 600);

        Rectangle groupBlock = new Rectangle(100, 100, 200, 150);
        groupBlock.setStyle("-fx-fill: lightblue; -fx-stroke: black;");

        currentGroup = new Group(watchDirs, Paths.get("target"));
        
        // STRING CONTAINED RULE
        Rectangle stringBlock = new Rectangle(10, 10, 150, 80);
        stringBlock.setStyle("-fx-fill: lightgreen; -fx-stroke: black;");
        TextField stringInput = new TextField("hello");
        stringInput.setLayoutX(15);
        stringInput.setLayoutY(20);
        CheckBox stringCase = new CheckBox("Case Sensitive");
        stringCase.setLayoutX(15);
        stringCase.setLayoutY(50);
        CheckBox stringRegex = new CheckBox("Regex");
        stringRegex.setLayoutX(15);
        stringRegex.setLayoutY(70);


        // EXTENSION RULE 
        Rectangle extBlock = new Rectangle(10, 100, 150, 50);
        extBlock.setStyle("-fx-fill: lightgreen; -fx-stroke: black;");
        TextField extInput = new TextField("png,jpg");
        extInput.setLayoutX(15);
        extInput.setLayoutY(110);


        // CATEGORY RULE
        Rectangle catBlock = new Rectangle(10, 160, 150, 50);
        catBlock.setStyle("-fx-fill: lightgreen; -fx-stroke: black;");
        ComboBox<String> catInput = new ComboBox<>();
        catInput.getItems().addAll("Image", "Document", "Audio", "Video");
        catInput.setValue("Image");
        catInput.setLayoutX(15);
        catInput.setLayoutY(170);

        // NAME HAS RULE
        Rectangle nameBlock = new Rectangle(10, 220, 150, 80);
        nameBlock.setStyle("-fx-fill: lightgreen; -fx-stroke: black;");
        TextField nameInput = new TextField("note");
        nameInput.setLayoutX(15);
        nameInput.setLayoutY(230);
        CheckBox nameCase = new CheckBox("Case Sensitive");
        nameCase.setLayoutX(15);
        nameCase.setLayoutY(260);
        CheckBox nameRegex = new CheckBox("Regex");
        nameRegex.setLayoutX(15);
        nameRegex.setLayoutY(280);


        // LAST ACCESSED RULE
        Rectangle lastAccessedBlock = new Rectangle(10, 280, 150, 50);
        lastAccessedBlock.setStyle("-fx-fill: lightgreen; -fx-stroke: black;");
        TextField daysInput = new TextField("7");
        daysInput.setLayoutX(15);
        daysInput.setLayoutY(290);

        Button finalizeButton = new Button("Finalize Group");
        finalizeButton.setLayoutX(100);
        finalizeButton.setLayoutY(340);
        finalizeButton.setOnAction(event -> {
            if (!currentGroup.getRules().isEmpty()) {
                if (!organizer.addGroup(currentGroup)) {
                    groupBlock.setStyle("-fx-fill: red; -fx-stroke: black;");
                } else {
                    groupBlock.setStyle("-fx-fill: lightblue; -fx-stroke: black;");
                    updateWatchDirectories();
                }
                currentGroup = new Group(watchDirs, Paths.get("target"));
            }
        });

        stringBlock.setOnDragDetected(e -> stringBlock.startDragAndDrop(TransferMode.ANY));
        extBlock.setOnDragDetected(e -> extBlock.startDragAndDrop(TransferMode.ANY));
        catBlock.setOnDragDetected(e -> catBlock.startDragAndDrop(TransferMode.ANY));
        nameBlock.setOnDragDetected(e -> nameBlock.startDragAndDrop(TransferMode.ANY));
        lastAccessedBlock.setOnDragDetected(e -> lastAccessedBlock.startDragAndDrop(TransferMode.ANY));

        groupBlock.setOnDragOver(event -> event.acceptTransferModes(TransferMode.ANY));
        groupBlock.setOnDragDropped(event -> {
            if (stringBlock.getBoundsInParent().intersects(event.getX(), event.getY(), 1, 1)) {
                currentGroup.addRule(new StringContainedRule(stringInput.getText(), stringCase.isSelected(), stringRegex.isSelected()));
            } else if (extBlock.getBoundsInParent().intersects(event.getX(), event.getY(), 1, 1)) {
                currentGroup.addRule(new FileExtensionRule(new HashSet<>(Arrays.asList(extInput.getText().split(",")))));
            } else if (catBlock.getBoundsInParent().intersects(event.getX(), event.getY(), 1, 1)) {
                currentGroup.addRule(new FileCategoryRule(catInput.getValue()));
            } else if (nameBlock.getBoundsInParent().intersects(event.getX(), event.getY(), 1, 1)) {
                currentGroup.addRule(new NameHasRule(nameInput.getText(), nameCase.isSelected(), nameRegex.isSelected()));
            } else if (lastAccessedBlock.getBoundsInParent().intersects(event.getX(), event.getY(), 1, 1)) {
                try {
                    int days = Integer.parseInt(daysInput.getText());
                    // Convert days to milliseconds and calculate the threshold timestamp
                    long daysInMillis = days * 24L * 60 * 60 * 1000; // days to milliseconds
                    long timeInMillis = System.currentTimeMillis() - daysInMillis;
                    currentGroup.addRule(new LastAccessedRule(timeInMillis));
                } catch (NumberFormatException e) {
                    System.out.println("Invalid number of days: " + daysInput.getText());
                }
            }
        });

        root.getChildren().addAll(groupBlock, stringBlock, extBlock, catBlock, nameBlock, lastAccessedBlock,
            stringInput, stringCase, stringRegex, extInput, catInput, nameInput, daysInput, finalizeButton);
        primaryStage.setScene(scene);
        primaryStage.setTitle("NeatFile");
        primaryStage.show();

        startFileWatcher();

        primaryStage.setOnCloseRequest(event -> {
           shutdown();
        });
    }

    private void startFileWatcher() {
        watchServiceThread = new Thread(() -> {
            try {
                WatchService watchService = FileSystems.getDefault().newWatchService();
                
                // enter initial directories
                updateWatchDirectories(watchService);

                while (running && !Thread.currentThread().isInterrupted()) {
                    WatchKey key;
                    try {
                        key = watchService.poll(1, TimeUnit.SECONDS);
                    } catch (InterruptedException e){
                        break;
                    }

                    if(key != null){
                        for (WatchEvent<?> event : key.pollEvents()) {
                            if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE ||
                                event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                                
                                Path fileName = (Path) event.context();
                                Path dir = (Path) key.watchable();
                                Path fullPath = dir.resolve(fileName);
                                organizer.processFile(fullPath);
                            }
                        }
                        if(!key.reset()) {
                            break;
                        };
                    }
                }
            } catch (IOException e) {
                System.err.println("WatchService error: " +e.getMessage());
            }
        })
        
        watchServiceThread.setDaemon(true);
        watchServiceThread.start();
    }

    private void shutdown(){
        running = false;
        if(watchServiceThread != null){
           watchServiceThread.interrupt();
           try {
                watchServiceThread.join(1000);
           } catch (InterruptedException e){
                Thread.currentThread().interrupt();
           }
        }
    }

    private void updateWatchDirectories(WatchService watchService) throws IOException {
        // 1. Is input valid
        Objects.requireNonNull(watchService, "WatchService cannot be empty");

        // 2. Valid directory and errors
        Set<Path> validDirs = new HashSet<>();
        for (Path dir : watchDirs) {
            try{
                if(!Files.isDirectory(dir)){
                    System.err.println("WARNING Not a directory: " + dir);
                    continue;
                }

                // 3. Register directory
                dir.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY
                    StandardWatchEventKinds.ENTRY_DELETE);

                validDirs.add(dir);
            } catch(IOException e){
                System.err.println("Error: Failed to watch " + dir + ": " + e.getMessage());
            }
        }

    // 4. Check valid directory
    if (validDirs.isEmpty()){
        throw new IOException("No valid directories to watch");
    }

    // 5. Update with only working directories
    watchDirs.retainAll(validDirs);
    
    public static void main(String[] args) {
        launch(args);
    }
}