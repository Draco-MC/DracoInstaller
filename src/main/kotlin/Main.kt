import com.formdev.flatlaf.FlatDarculaLaf
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import net.fabricmc.mappingio.format.proguard.ProGuardFileReader
import net.fabricmc.mappingio.format.tiny.Tiny2FileWriter
import net.fabricmc.mappingio.tree.MemoryMappingTree
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.*
import java.net.URI
import java.net.URL
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import javax.imageio.ImageIO
import javax.swing.*
import kotlin.io.path.createDirectory
import kotlin.io.path.deleteIfExists
import kotlin.io.path.toPath
import kotlin.io.path.writeBytes
import kotlin.system.exitProcess


class MainFrame : JFrame() {
    private lateinit var installClientButton: JButton
    private lateinit var installServerButton: JButton
    private lateinit var exitButton: JButton
    private lateinit var progressBar: JProgressBar
    private lateinit var progressLabel: JLabel
    private lateinit var loaderVersionBox: JTextField
    private lateinit var minecraftVersionBox: JTextField
    private lateinit var stdVersionBox: JTextField
    private lateinit var launcherType: JComboBox<String>
    private lateinit var manifest: JsonObject

    val libs = listOf(
        Pair("com.github.Draco-MC:LaunchWrapper:master-SNAPSHOT","https://jitpack.io/"),
        Pair("org.ow2.asm:asm:9.7","https://repository.ow2.org/nexus/service/local/repositories/releases/content/"),
        Pair("org.ow2.asm:asm-tree:9.7","https://repository.ow2.org/nexus/service/local/repositories/releases/content/"),
        Pair("org.ow2.asm:asm-util:9.7","https://repository.ow2.org/nexus/service/local/repositories/releases/content/"),
        Pair("org.ow2.asm:asm-commons:9.7","https://repository.ow2.org/nexus/service/local/repositories/releases/content/"),
        Pair("org.ow2.asm:asm-analysis:9.7","https://repository.ow2.org/nexus/service/local/repositories/releases/content/"),
        Pair("org.spongepowered:mixin:0.8.6","https://repo.spongepowered.org/maven/"),
        Pair("io.github.llamalad7:mixinextras-common:0.3.6","https://repo1.maven.org/maven2/"),
        Pair("org.jetbrains.kotlin:kotlin-stdlib:1.9.20","https://repo1.maven.org/maven2/"),
        Pair("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.20","https://repo1.maven.org/maven2/"),
        Pair("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.20","https://repo1.maven.org/maven2/"),
        Pair("net.fabricmc:access-widener:2.1.0","https://maven.fabricmc.net/"),
        Pair("com.formdev:flatlaf:3.4.1","https://repo1.maven.org/maven2/"),
    )

    init {
        setTitle("Draco Mod Loader Installer")
        setSize(500, 300)
        try {
            val stream = (URL("https://gist.githubusercontent.com/TalonFloof/e900366b17ebaf981a02d27f09bfd12c/raw/version_manifest.json").content as InputStream)
            manifest = Gson().fromJson(String(stream.readBytes()),JsonObject::class.java)
            stream.close()
        } catch (e: IOException) {
            playHandSound()
            JOptionPane.showMessageDialog(this,"Unable to download Standard Library Version Manifest","Error",JOptionPane.ERROR_MESSAGE)
        }
        isResizable = false
        setLocationRelativeTo(null)

        initComponent()
        initEvent()
    }

    private fun initComponent() {
        val banner = JLabel(ImageIcon(ImageIO.read(javaClass.classLoader.getResourceAsStream("draco-banner.png"))))
        banner.setBounds(0,0,300,120)
        val contentPane: Container = contentPane
        contentPane.setLayout(BorderLayout())
        contentPane.add(banner,BorderLayout.NORTH)
        val progressPane = JPanel(GridLayout(3,1))
        progressLabel = JLabel("To start, select a Draco version and a Minecraft version, then press install")
        progressBar = JProgressBar(0,100)
        progressPane.add(progressLabel)
        progressPane.add(progressBar)
        val installPane = JPanel(GridLayout(1,3))
        installClientButton = JButton("Install Client")
        installServerButton = JButton("Install Server (WIP)")
        exitButton = JButton("Exit")
        installPane.add(installClientButton)
        installPane.add(installServerButton)
        installPane.add(exitButton)
        progressPane.add(installPane)
        contentPane.add(progressPane,BorderLayout.SOUTH)
        val selectionPane = JPanel()
        selectionPane.layout = BoxLayout(selectionPane, BoxLayout.Y_AXIS)
        selectionPane.alignmentX = CENTER_ALIGNMENT
        selectionPane.alignmentY = CENTER_ALIGNMENT
        val label1 = JLabel("Draco Loader Version")
        loaderVersionBox = JTextField("main-SNAPSHOT")
        val grid1 = JPanel(GridBagLayout())
        loaderVersionBox.isEditable = true
        label1.alignmentX = Component.CENTER_ALIGNMENT
        grid1.add(label1)
        grid1.add(loaderVersionBox)
        selectionPane.add(grid1)
        val label2 = JLabel("Minecraft Version")
        minecraftVersionBox = JTextField("")
        val grid2 = JPanel(GridBagLayout())
        loaderVersionBox.isEditable = true
        label2.alignmentX = Component.CENTER_ALIGNMENT
        grid2.add(label2)
        grid2.add(minecraftVersionBox)
        val label3 = JLabel("Standard Library Version (Optional)")
        stdVersionBox = JTextField("@".repeat(10))
        val grid3 = JPanel(GridBagLayout())
        loaderVersionBox.isEditable = true
        label3.alignmentX = Component.CENTER_ALIGNMENT
        grid3.add(label3)
        grid3.add(stdVersionBox)
        selectionPane.add(grid3)
        selectionPane.add(grid2)
        val grid4 = JPanel(GridBagLayout())
        val label4 = JLabel("Launcher")
        launcherType = JComboBox<String>()
        launcherType.addItem("Vanilla")
        launcherType.addItem("Prism Launcher")
        label4.alignmentX = Component.CENTER_ALIGNMENT
        grid4.add(label4)
        grid4.add(launcherType)
        selectionPane.add(grid4)
        contentPane.add(selectionPane,BorderLayout.CENTER)
        pack()
        Thread {
            Thread.sleep(100)
            stdVersionBox.text = ""
            var prevText = minecraftVersionBox.text
            while(true) {
                Thread.sleep(500)
                if(prevText != minecraftVersionBox.text) {
                    prevText = minecraftVersionBox.text
                    if(manifest.has(prevText)) {
                        stdVersionBox.text = manifest.get(prevText).asString
                    } else {
                        stdVersionBox.text = ""
                    }
                }
            }
        }.start()
    }

    private fun playQuestionSound() {
        Toolkit.getDefaultToolkit().beep()
    }

    private fun playHandSound() {
        val hand = (Toolkit.getDefaultToolkit().getDesktopProperty("win.sound.hand") as Runnable?)
        if(hand != null) {
            hand.run()
        } else {
            Toolkit.getDefaultToolkit().beep()
        }
    }

    private fun initEvent() {
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                exitProcess(0)
            }
        })
        installClientButton.addActionListener { e -> installClient(e) }
        installServerButton.addActionListener { _ -> playHandSound(); JOptionPane.showMessageDialog(this,"Installation on the Minecraft Server JAR is currently unimplemented","Notice",JOptionPane.ERROR_MESSAGE) }
        exitButton.addActionListener { _ -> exitProcess(0) }
    }

    private fun installClient(event: ActionEvent) {
        SwingUtilities.invokeLater {
            Thread {
                installClientButton.isEnabled = false
                installServerButton.isEnabled = false
                exitButton.isEnabled = false
                minecraftVersionBox.isEnabled = false
                stdVersionBox.isEnabled = false
                loaderVersionBox.isEnabled = false
                progressBar.isIndeterminate = true
                launcherType.isEnabled = false
                progressLabel.text = "Preinstall checks..."
                if(stdVersionBox.text.isEmpty()) {
                    playQuestionSound()
                    if(JOptionPane.showConfirmDialog(this,"Are you sure you want to install DracoLoader without the Standard Library?\nAlthough it is not required, many mods rely on it in order to properly function.\nAlternatively, you can also load the Standard Library as a mod if you'd prefer to do that instead of having it embedded\n\nContinue with installation?","Warning",JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
                        abortInstall("Installation was aborted")
                        return@Thread
                    }
                }
                val os = System.getProperty("os.name").lowercase(Locale.ENGLISH)
                var minecraftPath: String = if (os.contains("win")) {
                    System.getenv("APPDATA") + File.separator + ".minecraft"
                } else if (os.contains("mac")) {
                    System.getProperty("user.home") + File.separator + "Library" + File.separator + "Application Support" + File.separator + "minecraft"
                } else {
                    System.getProperty("user.home") + File.separator + ".minecraft"
                }
                if(launcherType.selectedItem == "Vanilla") {
                    // Search for specified Minecraft Version
                    if (!File(minecraftPath).exists()) {
                        abortInstall("You don't appear to have the Minecraft Launcher installed (or you've never opened it) please open it and install the version you want to install Vulpes Loader to")
                        return@Thread
                    }
                }
                progressLabel.text = "Creating Temporary Download Directory..."
                val tempDir = File(System.getProperty("java.io.tmpdir") + File.separator + "VulpesInstaller")
                tempDir.deleteRecursively()
                tempDir.mkdir()
                progressLabel.text = "Downloading Minecraft..."
                try {
                    BufferedInputStream(URL("https://launchermeta.mojang.com/mc/game/version_manifest.json").openStream()).use { `in` ->
                        FileOutputStream(tempDir.absolutePath + File.separator + "version_manifest.json").use { fileOutputStream ->
                            val dataBuffer = ByteArray(1024)
                            var bytesRead: Int
                            while (`in`.read(dataBuffer, 0, 1024).also { bytesRead = it } != -1) {
                                fileOutputStream.write(dataBuffer, 0, bytesRead)
                            }
                        }
                    }
                    var jsonObj = Gson().fromJson(tempDir.resolve("version_manifest.json").readText(), JsonObject::class.java)
                    var found = false
                    jsonObj.get("versions").asJsonArray.forEach {
                        if(it.asJsonObject.get("id").asString.equals(minecraftVersionBox.text)) {
                            found = true
                            BufferedInputStream(URL(it.asJsonObject.get("url").asString).openStream()).use { `in` ->
                                FileOutputStream(tempDir.absolutePath + File.separator + "minecraft.json").use { fileOutputStream ->
                                    val dataBuffer = ByteArray(1024)
                                    var bytesRead: Int
                                    while (`in`.read(dataBuffer, 0, 1024).also { bytesRead = it } != -1) {
                                        fileOutputStream.write(dataBuffer, 0, bytesRead)
                                    }
                                }
                            }
                            var versionJson = Gson().fromJson(tempDir.resolve("minecraft.json").readText(), JsonObject::class.java)
                            val clientURL = versionJson.get("downloads").asJsonObject.get("client").asJsonObject.get("url").asString
                            BufferedInputStream(URL(clientURL).openStream()).use { `in` ->
                                FileOutputStream(tempDir.absolutePath + File.separator + minecraftVersionBox.text + ".jar").use { fileOutputStream ->
                                    val dataBuffer = ByteArray(1024)
                                    var bytesRead: Int
                                    while (`in`.read(dataBuffer, 0, 1024).also { bytesRead = it } != -1) {
                                        fileOutputStream.write(dataBuffer, 0, bytesRead)
                                    }
                                }
                            }
                            if(versionJson.get("downloads").asJsonObject.get("client_mappings") != null) {
                                val mappingsURL = versionJson.get("downloads").asJsonObject.get("client_mappings").asJsonObject.get("url")
                                BufferedInputStream(URL(mappingsURL.asString).openStream()).use { `in` ->
                                    FileOutputStream(tempDir.absolutePath + File.separator + "mappings.txt").use { fileOutputStream ->
                                        val dataBuffer = ByteArray(1024)
                                        var bytesRead: Int
                                        while (`in`.read(dataBuffer, 0, 1024).also { bytesRead = it } != -1) {
                                            fileOutputStream.write(dataBuffer, 0, bytesRead)
                                        }
                                    }
                                }
                                progressLabel.text = "Convert Mojmap from Proguard to Tiny Mappings..."
                                playQuestionSound()
                                if (JOptionPane.showConfirmDialog(
                                        this,
                                        "Please accept the following copyright notice to continue:\n\n(c) 2020 Microsoft Corporation. These mappings are provided \"as-is\" and you bear the risk of using them. You may copy and use the mappings for development purposes, but you may not redistribute the mappings complete and unmodified. Microsoft makes no warranties, express or implied, with respect to the mappings provided here.  Use and modification of this document or the source code (in any form) of Minecraft: Java Edition is governed by the Minecraft End User License Agreement available at https://account.mojang.com/documents/minecraft_eula.",
                                        "Deobfuscation Notice",
                                        JOptionPane.OK_CANCEL_OPTION
                                    ) == JOptionPane.CANCEL_OPTION
                                ) {
                                    abortInstall("User denied the copyright notice for the Mojang Mappings\nCannot continue");
                                    return@Thread
                                }
                                val tree = MemoryMappingTree()
                                ProGuardFileReader.read(
                                    FileReader(tempDir.absolutePath + File.separator + "mappings.txt"),
                                    "mojmap",
                                    "notch",
                                    tree
                                )
                                tree.accept(
                                    Tiny2FileWriter(
                                        OutputStreamWriter(FileOutputStream(tempDir.absolutePath + File.separator + "mappings.tiny")),
                                        false
                                    )
                                )
                            } else {
                                tempDir.deleteRecursively()
                                abortInstall("This version of Minecraft does not have Mojang Mappings available for it, only versions 1.14.4 or later support Mojang Mappings")
                                return@Thread
                            }
                        }
                    }
                    if(!found) {
                        tempDir.deleteRecursively()
                        abortInstall("Couldn't download that version of Minecraft!")
                        return@Thread
                    }
                } catch (e: Exception) {
                    tempDir.deleteRecursively()
                    abortInstall(e.toString())
                    return@Thread
                }
                progressLabel.text = "Deobfuscating Minecraft..."
                try {
                    BufferedInputStream(URL("https://maven.fabricmc.net/net/fabricmc/tiny-remapper/0.9.0/tiny-remapper-0.9.0-fat.jar").openStream()).use { `in` ->
                        FileOutputStream(tempDir.absolutePath + File.separator + "tiny-remapper.jar").use { fileOutputStream ->
                            val dataBuffer = ByteArray(1024)
                            var bytesRead: Int
                            while (`in`.read(dataBuffer, 0, 1024).also { bytesRead = it } != -1) {
                                fileOutputStream.write(dataBuffer, 0, bytesRead)
                            }
                        }
                    }
                    val pb = ProcessBuilder("java", "-jar", tempDir.absolutePath + File.separator + "tiny-remapper.jar", tempDir.absolutePath + File.separator + minecraftVersionBox.text + ".jar", tempDir.absolutePath + File.separator + minecraftVersionBox.text + "-deobf.jar", tempDir.absolutePath + File.separator + "mappings.tiny", "notch", "mojmap")
                    pb.directory(tempDir)
                    pb.redirectOutput(File("out.log"))
                    pb.redirectError(File("err.log"))
                    if(pb.start().waitFor() != 0) {
                        tempDir.deleteRecursively()
                        abortInstall("Tiny Remapper exited with non-zero value!")
                        return@Thread
                    }
                } catch(e: Exception) {
                    tempDir.deleteRecursively()
                    abortInstall(e.toString())
                    return@Thread
                }
                var jsonObj = Gson().fromJson(tempDir.resolve("minecraft.json").readText(), JsonObject::class.java)
                if(launcherType.selectedItem == "Vanilla") {
                    progressLabel.text = "Creating Manifest JSON..."
                    jsonObj.remove("downloads")
                    var libraries = jsonObj.getAsJsonArray("libraries")
                    // Add Libraries
                    run {
                        val library = JsonObject()
                        library.add("name", JsonPrimitive("com.github.Draco-MC:DracoLoader:"+loaderVersionBox.text))
                        library.add("url", JsonPrimitive("https://jitpack.io/"))
                        libraries.add(library)
                    }
                    if(stdVersionBox.text.isNotEmpty()) {
                        val library = JsonObject()
                        library.add("name", JsonPrimitive("com.github.Draco-MC:DracoStandardLibrary:"+stdVersionBox.text))
                        library.add("url", JsonPrimitive("https://jitpack.io/"))
                        libraries.add(library)
                    }
                    for(lib in libs) {
                        val library = JsonObject()
                        library.addProperty("name",lib.first)
                        library.addProperty("url",lib.second)
                        libraries.add(library)
                    }
                    jsonObj.add("mainClass",JsonPrimitive("net.minecraft.launchwrapper.Launch"))
                    run {
                        val a = jsonObj.getAsJsonObject("arguments").getAsJsonArray("game")
                        a.add("--tweakClass")
                        a.add("sh.talonfloof.dracoloader.bootstrap.MinecraftClientBootstrap")
                    }
                    run {
                        val a = jsonObj.getAsJsonObject("arguments").getAsJsonArray("jvm")
                        a.add("-Dlegacy.debugClassLoading\\u003dtrue")
                    }
                    jsonObj.add("id",JsonPrimitive(jsonObj.get("id").asString!!+"-draco"))
                    progressLabel.text = "Creating Minecraft Version Directory..."
                    var ver =
                        File(minecraftPath + File.separator + "versions" + File.separator + minecraftVersionBox.text + "-draco")
                    ver.deleteRecursively()
                    ver.mkdir()
                    ver.resolve(minecraftVersionBox.text + "-draco.json")
                        .writeText(Gson().newBuilder().setPrettyPrinting().create().toJson(jsonObj))
                    progressLabel.text = "Moving Deobfuscated Minecraft JAR to Version Directory..."
                    tempDir.resolve(minecraftVersionBox.text + "-deobf.jar")
                        .renameTo(ver.resolve(minecraftVersionBox.text + "-draco.jar"))
                } else if(launcherType.selectedItem == "Prism Launcher") {
                    progressLabel.text = "Constructing Prism Launcher Instance..."
                    tempDir.resolve("mmc-pack.json").writeText("{\n" +
                            "    \"components\": [\n" +
                            "        {\n" +
                            "            \"important\": true,\n" +
                            "            \"uid\": \"net.minecraft\",\n" +
                            "            \"version\": \""+minecraftVersionBox.text+"\"\n" +
                            "        },\n" +
                            "        {\n" +
                            "            \"cachedName\": \"Draco Mod Loader\",\n" +
                            "            \"uid\": \"customjar\"\n" +
                            "        }\n" +
                            "    ],\n" +
                            "    \"formatVersion\": 1\n" +
                            "}\n")
                    tempDir.resolve("instance.cfg").writeText("[General]\n" +
                            "ConfigVersion=1.2\n" +
                            "iconKey=draco\n" +
                            "name=Draco\n")
                    tempDir.resolve("patches").mkdir()
                    val customJarJson = JsonObject()
                    customJarJson.addProperty("formatVersion",1)
                    run {
                        val obj = JsonObject()
                        obj.addProperty("MMC-displayname","Draco Mod Loader")
                        obj.addProperty("MMC-hint", "local")
                        obj.addProperty("name","custom:customjar:1")
                        customJarJson.add("mainJar",obj)
                    }
                    customJarJson.addProperty("name","Draco Mod Loader")
                    customJarJson.addProperty("uid","customjar")
                    customJarJson.addProperty("mainClass","net.minecraft.launchwrapper.Launch")
                    customJarJson.addProperty("minecraftArguments","--username \${auth_player_name} --version \${version_name} --gameDir \${game_directory} --assetsDir \${assets_root} --assetIndex \${assets_index_name} --uuid \${auth_uuid} --accessToken \${auth_access_token} --userType \${user_type} --versionType \${version_type} --tweakClass sh.talonfloof.dracoloader.bootstrap.MinecraftClientBootstrap")
                    run {
                        val libraries = JsonArray()
                        if (stdVersionBox.text.isNotEmpty()) {
                            val library = JsonObject()
                            library.add(
                                "name",
                                JsonPrimitive("com.github.Draco-MC:DracoStandardLibrary:" + stdVersionBox.text)
                            )
                            library.add("url", JsonPrimitive("https://jitpack.io/"))
                            libraries.add(library)
                        }
                        run {
                            val library = JsonObject()
                            library.add("name", JsonPrimitive("com.github.Draco-MC:DracoLoader:"+loaderVersionBox.text))
                            library.add("url", JsonPrimitive("https://jitpack.io/"))
                            libraries.add(library)
                        }
                        for (lib in libs) {
                            val library = JsonObject()
                            library.addProperty("name", lib.first)
                            library.addProperty("url", lib.second)
                            libraries.add(library)
                        }
                        customJarJson.add("libraries",libraries)
                    }
                    tempDir.resolve("patches").resolve("customjar.json").writeText(Gson().newBuilder().setPrettyPrinting().create().toJson(customJarJson))
                    val uri = URI.create("jar:"+File("Draco.zip").toURI().toString())
                    if(File("Draco.zip").exists()) File("Draco.zip").delete()
                    FileSystems.newFileSystem(uri, mutableMapOf(Pair("create", "true"))).use { zipfs ->
                        zipfs.getPath("/libraries").createDirectory()
                        zipfs.getPath("/patches").createDirectory()
                        zipfs.getPath("/.minecraft").createDirectory()
                        Files.copy(tempDir.resolve(minecraftVersionBox.text+"-deobf.jar").toPath(),zipfs.getPath("/libraries/customjar-1.jar"),
                            StandardCopyOption.REPLACE_EXISTING)
                        Files.copy(tempDir.resolve("patches").resolve("customjar.json").toPath(),zipfs.getPath("/patches/customjar.json"),
                            StandardCopyOption.REPLACE_EXISTING)
                        Files.copy(tempDir.resolve("mmc-pack.json").toPath(),zipfs.getPath("/mmc-pack.json"),
                            StandardCopyOption.REPLACE_EXISTING)
                        Files.copy(tempDir.resolve("instance.cfg").toPath(),zipfs.getPath("/instance.cfg"),
                            StandardCopyOption.REPLACE_EXISTING)
                        val bytes = this.javaClass.getResourceAsStream("draco-icon.png")!!.readBytes()
                        zipfs.getPath("/draco.png").writeBytes(bytes)
                        zipfs.getPath("/.minecraft/icon.png").writeBytes(bytes)
                    }
                }
                progressLabel.text = "Cleaning up..."
                tempDir.deleteRecursively()
                progressLabel.text = "Installation Successful!"
                playQuestionSound()
                if(launcherType.selectedItem == "Vanilla") {
                    if (JOptionPane.showConfirmDialog(
                            this,
                            "Draco Loader has been successfully installed!\nWould you like to add a new profile for Draco?",
                            "Installation Successful!",
                            JOptionPane.YES_NO_OPTION
                        ) == JOptionPane.YES_OPTION
                    ) {
                        jsonObj = Gson().fromJson(
                            File(minecraftPath).resolve("launcher_profiles.json").readText(),
                            JsonObject::class.java
                        )
                        var profile = JsonObject()
                        profile.add("created", JsonPrimitive("1970-01-02T00:00:00.000Z"))
                        profile.add("lastUsed", JsonPrimitive("1970-01-02T00:00:00.000Z"))
                        profile.add("lastVersionId", JsonPrimitive(minecraftVersionBox.text + "-draco"))
                        profile.add("name", JsonPrimitive("Draco (" + minecraftVersionBox.text + ")"))
                        profile.add("type", JsonPrimitive("custom"))
                        profile.add(
                            "icon", JsonPrimitive(
                                "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAIAAAACACAYAAADDPmHLAAAAAXNSR0IArs4c6QAAA6JJREFUeJztnT1qGDEQRtfGx3GTAxiMqxhyiZTp3eQEbty7zCUCcWUCOUCa3CdpvEEYsdLqb0b63qvMGswaRm8+aaXdiw2qubv//Pf9tdcf3y4s7uUsl9Y3ALZQAOJcWd/AbMR0PzMYQBwKQBxaQAaraT8EA4iDAQ5YeeTvYABxKABxaAFvKOg+BgYQR9oAJaP+y9PX/z8/Pzw2vR8LMIA4FIA4ki3grPpD7a8GBhBneQMw2o/BAOJQAOIs0wJqV/Jy1b/C3D8EA4hDAYgzfQuoUX/PxB/el+czAhhAnKkM4HW0p9jv26MJMIA4FIA47pS0M5Pua9cGLFsDBhDHlQFqd+h4osQKFibAAOJQAOKYt4Az2q/RfUrJPVuJ53aAAcRxb4CSkVky4kaESY8mwADiUADimD0Maq1+r9r3DgYQxywE1hhghdHuJRBiAHEoAHGm2hFUgtf5fQk9dhZhAHEoAHGGtoDauX+uar0lfs9gAHGWCYGWo37m9wZhAHEoAHGWaQFKtDx3iAHEmd4A3qZ8+/2kwuD1p49Zf+/P95fqezoCA4hDAYgzpAUcrQCeUbg33c8254+BAcSZPgSOptWoD8PdUSAMf9cjEGIAcSgAcbq1gB4nfqzwHPZqdwlhAHGaG8DDx5diI3bUGcNYoIuFt9yVwN5gAHEoAHGatIBc7fcIfrmatgxyXnQfAwOIQwGIU9wCRr3bJ4bneflsYABxuq0Ethr1PY5Rt16r8BzyUmAAcSgAcZocM2714Kcm3LU6Mn2mPYxQf2oPANvCoQrzHUFe3pVTEgz30UkIhGmhAMQZqtJYGEy1AC+6z6V1OyAEQlfMQuDoke9hp5JHMIA4FIA4Q1uAxWPc1ruVRv0PR+GPF0VCMygAcZq3gF1Po1P3Sh+dHAkGEKdbCAyDSk8bMPLrwADiUADiuPpmUO78drT2W73yLUXvBz8xMIA4Zg+DSqrZ8gziqmAAcSgAccw3habwfPR85s2gOxhAHPcGSGEZ+G5uP2zbtm2/fv4uvvb++mgwgDgUgDhmK4EpWr1h/CyWwY+VQBiOqxA4w9btWHiruRZe7/15mBgYQBwKQBxXLSCFh4c8NXN+1gHAHRSAOOYtwMuHJY7m/+Hcn1kALIW5AWYgNXIJgTAtFIA4tIAMUupuFQJDYoGw9gthMTCAOBggg1EhMLx2NCUMp868JQyqoADEMd8RNMMegBng07FQBAUgDgUgDgUgDgUgDgUgzj/ADZhNYVy/6gAAAABJRU5ErkJggg=="
                            )
                        )
                        jsonObj.getAsJsonObject("profiles").add(minecraftVersionBox.text + "-draco", profile)
                        File(minecraftPath).resolve("launcher_profiles.json")
                            .writeText(Gson().newBuilder().setPrettyPrinting().create().toJson(jsonObj))
                        playQuestionSound()
                        JOptionPane.showMessageDialog(
                            this,
                            "Profile was successfully created\nIf the Minecraft Launcher is currently running then you'll need to restart it to see the new profile",
                            "Successfully Added Profile!",
                            JOptionPane.INFORMATION_MESSAGE
                        )
                    }
                } else if(launcherType.selectedItem == "Prism Launcher") {
                    JOptionPane.showMessageDialog(this,"Draco Loader has been successfully installed!\nTo use Draco Loader with Prism Launcher, import the newly created file Draco.zip by clicking \"Add Instance\" and then going to the \"Import\" section", "Installation Successful!", JOptionPane.INFORMATION_MESSAGE)
                }
                progressBar.isIndeterminate = false
                installClientButton.isEnabled = true
                installServerButton.isEnabled = true
                exitButton.isEnabled = true
                stdVersionBox.isEnabled = true
                minecraftVersionBox.isEnabled = true
                loaderVersionBox.isEnabled = true
                launcherType.isEnabled = true
                progressLabel.text = "To start, select a Draco version and a Minecraft version, then press install"
            }.start()
        }
    }

    private fun abortInstall(reason: String) {
        playHandSound()
        JOptionPane.showMessageDialog(this,reason,"Installation Failed!",JOptionPane.ERROR_MESSAGE)
        installClientButton.isEnabled = true
        installServerButton.isEnabled = true
        exitButton.isEnabled = true
        minecraftVersionBox.isEnabled = true
        stdVersionBox.isEnabled = true
        loaderVersionBox.isEnabled = true
        progressBar.isIndeterminate = false
        launcherType.isEnabled = true
        progressLabel.text = "To start, select a Vulpes version and a Minecraft version, then press install"
    }
}

fun main(args: Array<String>) {
    JFrame.setDefaultLookAndFeelDecorated( true );
    JDialog.setDefaultLookAndFeelDecorated( true );
    System.setProperty("flatlaf.useWindowDecorations","true")
    System.setProperty("flatlaf.menuBarEmbedded","true")
    FlatDarculaLaf.setup()
    var f = MainFrame()
    f.isVisible = true
}
