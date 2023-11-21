package com.example;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Observer Interface
interface FileConversionObserver {
    void update(String message);
}

// Singleton Observer
class FileConversionLogger implements FileConversionObserver {
    private static FileConversionLogger instance;

    private FileConversionLogger() {
        // Private constructor to prevent instantiation
    }

    public static synchronized FileConversionLogger getInstance() {
        if (instance == null) {
            instance = new FileConversionLogger();
        }
        return instance;
    }

    @Override
    public void update(String message) {
        System.out.println(message);
    }
}

// Factory Interface
interface FileConverterFactory {
    FileConverter createConverter(String outputFormat);
}

// Concrete Factory
class ImageConverterFactory implements FileConverterFactory {
    @Override
    public FileConverter createConverter(String outputFormat) {
        if (outputFormat.equalsIgnoreCase("png")) {
            return new ImageToPngConverter();
        } else if (outputFormat.equalsIgnoreCase("jpg")) {
            return new ImageToJpgConverter();
        }
        return null;
    }
}

// Strategy Interface
interface FileConverter {
    void convert(String inputFile, String outputFile);
}

// Concrete Strategies
class ImageToPngConverter implements FileConverter {
    @Override
    public void convert(String inputFilePath, String outputFilePath) {
        try {
            BufferedImage bufferedImage = ImageIO.read(new File(inputFilePath));
            ImageIO.write(bufferedImage, "png", new File(outputFilePath));
            FileConversionLogger.getInstance().update("JPG to PNG conversion completed: " + inputFilePath);
        } catch (IOException e) {
            FileConversionLogger.getInstance().update("Error converting JPG to PNG: " + inputFilePath);
        }
    }
}

class ImageToJpgConverter implements FileConverter {
    @Override
    public void convert(String inputFilePath, String outputFilePath) {
        try {
            BufferedImage bufferedImage = ImageIO.read(new File(inputFilePath));
            ImageIO.write(bufferedImage, "jpg", new File(outputFilePath));
            FileConversionLogger.getInstance().update("PNG to JPG conversion completed: " + inputFilePath);
        } catch (IOException e) {
            FileConversionLogger.getInstance().update("Error converting PNG to JPG: " + inputFilePath);
        }
    }
}

// Decorator
class ObservableFileConverter implements FileConverter {
    private FileConverter fileConverter;
    private FileConversionObserver observer;

    public ObservableFileConverter(FileConverter fileConverter, FileConversionObserver observer) {
        this.fileConverter = fileConverter;
        this.observer = observer;
    }

    @Override
    public void convert(String inputFile, String outputFile) {
        try {
            fileConverter.convert(inputFile, outputFile);
            observer.update("Conversion successful: " + inputFile);
        } catch (Exception e) {
            observer.update("Error converting file: " + inputFile);
        }
    }
}

class FileProcessor {
    private List<String> fileList;
    private Pattern pattern;

    public FileProcessor(List<String> fileList, Pattern pattern) {
        this.fileList = fileList;
        this.pattern = pattern;
    }

    public void processFile(Path path) {
        String fileName = path.getFileName().toString();
        Matcher matcher = pattern.matcher(fileName);

        if (matcher.matches()) {
            fileList.add(fileName);
        }
    }
}

// Adapter
class FileAdapter {
    /**
     * @param regex
     * @return
     */
    public static List<String> getFilesFromRegex(String regex) {
        List<String> fileList = new ArrayList<>();

        try {
            // Define the base directory where you want to search for files
            Path basePath = Paths.get("./");

            // Create a regex pattern
            Pattern pattern = Pattern.compile(regex);

            // Create an instance of FileProcessor
            final FileProcessor fileProcessor = new FileProcessor(fileList, pattern);

            // Traverse the base directory and match files against the regex
            Files.walkFileTree(basePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                    if (Files.isRegularFile(path)) {
                        fileProcessor.processFile(path);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    // Handle file visit failure if necessary
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            // Handle IOException
            e.printStackTrace();
        }

        return fileList;
    }
}

// CLI Command
public class mainApp {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java FileConverterCommand <outputFormat> <file1> [<file2> ...]");
            System.exit(1);
        }

        String outputFormat = args[0];
        List<String> filesToConvert = new ArrayList<>();

        // Use Adapter to generate list of files from regex/path/etc.
        for (int i = 1; i < args.length; i++) {
            if (args[i].matches("[a-zA-Z0-9]*\\.[a-zA-Z0-9]*")) {
                // If argument is a file name, add it directly
                filesToConvert.add(args[i]);
            } else {
                // If argument is a regex, use the adapter to fetch files
                List<String> matchingFiles = FileAdapter.getFilesFromRegex(args[i]);
                filesToConvert.addAll(matchingFiles);
            }
        }
        System.out.println("Files: " + filesToConvert.toString());

        // Use Factory to create appropriate converter
        FileConverterFactory converterFactory = new ImageConverterFactory();
        FileConverter fileConverter = converterFactory.createConverter(outputFormat);

        // Use Observer and Decorator for logging
        FileConversionLogger logger = FileConversionLogger.getInstance();
        fileConverter = new ObservableFileConverter(fileConverter, logger);

        // Use Strategy pattern for conversion
        for (String inputFile : filesToConvert) {
            String outputFile = generateOutputFilePath(inputFile, outputFormat);
            fileConverter.convert(inputFile, outputFile);
        }
    }

    private static String generateOutputFilePath(String inputFile, String outputFormat) {
        int lastDotIndex = inputFile.lastIndexOf(".");
        String fileNameWithoutExtension = lastDotIndex != -1 ? inputFile.substring(0, lastDotIndex) : inputFile;
        return fileNameWithoutExtension + "." + outputFormat;
    }
}
