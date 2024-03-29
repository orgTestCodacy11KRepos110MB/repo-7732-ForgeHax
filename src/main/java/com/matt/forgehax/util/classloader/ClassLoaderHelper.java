package com.matt.forgehax.util.classloader;

import static com.matt.forgehax.util.FileHelper.*;

import com.google.common.collect.Lists;
import com.matt.forgehax.ForgeHax;
import com.matt.forgehax.util.Streamables;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import sun.net.www.protocol.file.FileURLConnection;

public class ClassLoaderHelper {
  private static void searchDirectory(
      final Path directory, final Function<Path, Boolean> function) {
    Optional.ofNullable(directory)
        .filter(Files::exists)
        .filter(Files::isDirectory)
        .ifPresent(
            dir -> {
              try {
                Files.list(dir)
                    .forEach(
                        path -> {
                          if (function.apply(path)) searchDirectory(path, function);
                        });
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
  }

  public static List<Path> getJarPathsInDirectory(final Path directory, boolean recursive) {
    final List<Path> results = Lists.newArrayList();
    searchDirectory(
        directory,
        path -> {
          if (Files.isDirectory(path)) return recursive;
          else if (getFileExtension(path).equals("jar")) return results.add(directory);
          return false;
        });
    return results;
  }

  public static List<Path> getJarPathsInDirectory(File directory, boolean recursive) {
    return getJarPathsInDirectory(directory.toPath(), recursive);
  }

  public static List<Path> getJarPathsInDirectory(String directory, boolean recursive) {
    return getJarPathsInDirectory(Paths.get(directory), recursive);
  }

  public static List<Path> getJarPathsInDirectory(String directory) {
    boolean recursive = directory.endsWith("/*") || directory.endsWith("\\*");
    return getJarPathsInDirectory(
        Paths.get(recursive ? directory.substring(0, directory.length() - 2) : directory),
        recursive);
  }

  /**
   * Generates a list of all files inside the directory (recursively).
   *
   * @param directory root directory to initiate scan
   * @param recursive if the scan should look into sub directories
   * @return All files inside the directory that have the .class extension
   */
  public static List<Path> getClassPathsInDirectory(Path directory, boolean recursive) {
    final List<Path> results = Lists.newArrayList();
    searchDirectory(
        directory,
        path -> {
          if (Files.isDirectory(path)) return recursive;
          else if (getFileExtension(path).equals("class")) results.add(path);
          return false;
        });
    return results;
  }

  public static List<Path> getClassPathsInDirectory(File directory, boolean recursive) {
    return getClassPathsInDirectory(directory.toPath(), recursive);
  }

  public static List<Path> getClassPathsInDirectory(String directory, boolean recursive) {
    return getClassPathsInDirectory(Paths.get(directory), recursive);
  }

  public static List<Path> getClassPathsInDirectory(String directory) {
    boolean recursive = directory.endsWith("/*") || directory.endsWith("\\*");
    return getClassPathsInDirectory(
        Paths.get(recursive ? directory.substring(0, directory.length() - 2) : directory),
        recursive);
  }

  /**
   * Generates a list of all the paths that have the file extension '.class'
   *
   * @param jarFile jar file
   * @param packageDir path to the package
   * @param recursive if the scan should look into sub directories
   * @return a list of class paths
   * @throws IOException if there is an issue opening/reading the files
   */
  public static List<Path> getClassPathsInJar(JarFile jarFile, String packageDir, boolean recursive)
      throws IOException {
    Objects.requireNonNull(jarFile);
    Objects.requireNonNull(packageDir);

    // open new file system to the jar file
    final FileSystem fs = newFileSystem(jarFile.getName());
    final Path root = fs.getRootDirectories().iterator().next();
    final Path packagePath = root.resolve(packageDir);

    return Streamables.enumerationStream(jarFile.entries())
        .map(entry -> root.resolve(entry.getName()))
        .filter(path -> getFileExtension(path).equals("class"))
        .filter(
            path ->
                recursive
                    || path.getNameCount()
                        == packagePath.getNameCount()
                            + 1) // name count = directories, +1 for the class file name
        .filter(
            path ->
                path.toString().startsWith(path.getFileSystem().getSeparator() + packageDir)
                    && path.toString().length()
                        > (packageDir.length() + 2)) // 2 = root (first) '/' + suffix '/'
        .collect(Collectors.toList());
  }

  public static List<Path> getClassPathsInJar(File file, String packagePath, boolean recursive)
      throws IOException {
    Objects.requireNonNull(file);
    return getClassPathsInJar(new JarFile(file), packagePath, recursive);
  }

  public static List<Path> getClassPathsInJar(Path path, String packagePath, boolean recursive)
      throws IOException {
    Objects.requireNonNull(path);
    return getClassPathsInJar(path.toFile(), packagePath, recursive);
  }

  public static List<Path> getClassPathsInJar(JarFile jarFile, String packageDir)
      throws IOException {
    boolean recursive = packageDir.endsWith(".*");
    return getClassPathsInJar(
        jarFile,
        recursive ? packageDir.substring(0, packageDir.length() - 2) : packageDir,
        recursive);
  }

  public static List<Path> getClassPathsInJar(File file, String packagePath) throws IOException {
    Objects.requireNonNull(file);
    return getClassPathsInJar(new JarFile(file.getPath()), packagePath);
  }

  public static List<Path> getClassPathsInJar(Path path, String packagePath) throws IOException {
    Objects.requireNonNull(path);
    return getClassPathsInJar(path.toFile(), packagePath);
  }

  /**
   * Will attempt to find every class inside the package recursively.
   *
   * @param classLoader class loader to get the package resource from
   * @param packageIn name of package to search
   * @param recursive if the scan should look into sub directories
   * @return list of all the classes found
   * @throws IOException
   */
  // TODO: clean this up
  // TODO: this really needs to be rewritten
  public static List<Path> getClassPathsInPackage(
      final ClassLoader classLoader, final String packageIn, final boolean recursive)
      throws IOException {
    Objects.requireNonNull(packageIn);
    Objects.requireNonNull(classLoader);


    // package directory
    final String jarPackageDir = asFilePath(packageIn, "/");
    final String packageFileDir = asFilePath(packageIn, File.separator);

    final String forgehaxMainPath = ForgeHax.class.getName().replace('.', '/') + ".class";
    final URL url = classLoader.getResource(forgehaxMainPath);

    try {
      final URLConnection connection = url.openConnection();


      if (connection.getClass().getName().contains("ModJarURLHandler$ModJarURLConnection")) {
        final ModFile modFile = FMLLoader.getLoadingModList().getModFileById(connection.getURL().getHost()).getFile();

        final Path jarPath = modFile.getFilePath();
        if (Files.isRegularFile(jarPath)) {
          return getClassPathsInJar(new JarFile(jarPath.toFile()), jarPackageDir, recursive);
        } else {
          final Path buildPath = modFile.findResource(packageFileDir);
          // this is kind of gay but i guess that's just how it has to be
          final Path buildRootPath = Paths.get(buildPath.toString().substring(0, buildPath.toString().indexOf(packageFileDir)));

          return getClassPathsInDirectory(buildPath, recursive)
              .stream()
              .map(buildRootPath::relativize)
              .collect(Collectors.toList());
        }
        // poz below
      } else {
        // get the path to the jar/folder containing the classes
        String path = URLDecoder.decode(url.getPath(), "UTF-8")
            .replace('\\', '/'); // get path and covert backslashes to forward slashes
        path = path.substring(path.indexOf('/') + 1); // remove the initial '/' or 'file:/' appended to the path
        path = path.substring(0, path.indexOf(forgehaxMainPath)); // remove com/matt/forgehax/Forgehax.class
        path += jarPackageDir;


        // the root directory to the jar/folder containing the classes
        final String rootDir = path.substring(0, path.indexOf(jarPackageDir));


        if (connection instanceof FileURLConnection) { // FileURLConnection doesn't seem to be used
          final Path root = Paths.get(rootDir).normalize();
          return getClassPathsInDirectory(path, recursive)
              .stream()
              .map(root::relativize)
              .collect(Collectors.toList());

        } else if (connection instanceof JarURLConnection) {
          return getClassPathsInJar(((JarURLConnection) connection).getJarFile(), jarPackageDir, recursive);
        }

      }

        throw new UnknownConnectionTypeException(connection.getClass());

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static List<Path> getClassPathsInPackage(final ClassLoader classLoader, String packageDir)
      throws IOException {
    boolean recursive = packageDir.endsWith(".*");
    return getClassPathsInPackage(
        classLoader,
        recursive ? packageDir.substring(0, packageDir.length() - 2) : packageDir,
        recursive);
  }

  public static List<Class<?>> getLoadedClasses(
      final ClassLoader classLoader, Collection<Path> paths) {
    Objects.requireNonNull(classLoader);
    Objects.requireNonNull(paths);
    return paths
        .stream()
        .map(
            path -> {
              try {
                return Class.forName(asPackagePath(path), false, classLoader);
              } catch (ClassNotFoundException e) {
                return null;
              }
            })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  public static class UnknownConnectionTypeException extends Exception {

    public UnknownConnectionTypeException(Class<? extends URLConnection> type) {
      super(type.toString());
    }
  }
}
