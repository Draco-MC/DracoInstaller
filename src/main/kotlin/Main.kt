import com.formdev.flatlaf.FlatDarculaLaf
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import net.fabricmc.mappingio.MappedElementKind
import net.fabricmc.mappingio.extras.MappingTreeRemapper
import net.fabricmc.mappingio.format.proguard.ProGuardFileReader
import net.fabricmc.mappingio.format.tiny.Tiny1FileReader
import net.fabricmc.mappingio.format.tiny.Tiny1FileWriter
import net.fabricmc.mappingio.format.tiny.Tiny2FileReader
import net.fabricmc.mappingio.format.tiny.Tiny2FileWriter
import net.fabricmc.mappingio.tree.MemoryMappingTree
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.*
import java.net.URL
import java.util.*
import javax.imageio.ImageIO
import javax.swing.*
import kotlin.system.exitProcess


class MainFrame : JFrame() {
    private lateinit var installClientButton: JButton
    private lateinit var installServerButton: JButton
    private lateinit var exitButton: JButton
    private lateinit var progressBar: JProgressBar
    private lateinit var progressLabel: JLabel
    private lateinit var loaderVersionBox: JTextField
    private lateinit var minecraftVersionBox: JTextField
    init {
        setTitle("Draco Mod Loader Installer")
        setSize(500, 300)
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
        selectionPane.add(grid2)
        contentPane.add(selectionPane,BorderLayout.CENTER)
        pack()
    }

    private fun initEvent() {
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                exitProcess(0)
            }
        })
        installClientButton.addActionListener { e -> installClient(e) }
        installServerButton.addActionListener { _ -> JOptionPane.showMessageDialog(this,"Installation on the Minecraft Server JAR is currently unimplemented","Notice",JOptionPane.ERROR_MESSAGE) }
        exitButton.addActionListener { _ -> exitProcess(0) }
    }

    private fun installClient(event: ActionEvent) {
        SwingUtilities.invokeLater {
            Thread {
                installClientButton.isEnabled = false
                installServerButton.isEnabled = false
                exitButton.isEnabled = false
                minecraftVersionBox.isEnabled = false
                loaderVersionBox.isEnabled = false
                progressBar.isIndeterminate = true
                progressLabel.text = "Searching for Minecraft Instance..."
                // Search for specified Minecraft Version
                val os = System.getProperty("os.name").lowercase(Locale.ENGLISH)
                var minecraftPath: String = if (os.contains("win")) {
                    System.getenv("APPDATA") + File.separator + ".minecraft"
                } else if (os.contains("mac")) {
                    System.getProperty("user.home") + File.separator + "Library" + File.separator + "Application Support" + File.separator + "minecraft"
                } else {
                    System.getProperty("user.home") + File.separator + ".minecraft"
                }
                if (!File(minecraftPath).exists()) {
                    abortInstall("You don't appear to have the Minecraft Launcher installed (or you've never opened it) please open it and install the version you want to install Vulpes Loader to")
                    return@Thread
                }
                progressLabel.text = "Creating Temporary Download Directory..."
                val tempDir = File(System.getProperty("java.io.tmpdir") + File.separator + "VulpesInstaller")
                tempDir.deleteRecursively()
                tempDir.mkdir()
                progressLabel.text = "Convert Mojmap from Proguard to Tiny Mappings..."
                if(JOptionPane.showConfirmDialog(this,"Please accept the following copyright notice to continue:\n\n(c) 2020 Microsoft Corporation. These mappings are provided \"as-is\" and you bear the risk of using them. You may copy and use the mappings for development purposes, but you may not redistribute the mappings complete and unmodified. Microsoft makes no warranties, express or implied, with respect to the mappings provided here.  Use and modification of this document or the source code (in any form) of Minecraft: Java Edition is governed by the Minecraft End User License Agreement available at https://account.mojang.com/documents/minecraft_eula.","Deobfuscation Notice",JOptionPane.OK_CANCEL_OPTION) == JOptionPane.CANCEL_OPTION) {
                    abortInstall("User denied the copyright notice for the Mojang Mappings\nCannot continue");
                    return@Thread
                }
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
                            val mappingsURL = versionJson.get("downloads").asJsonObject.get("client_mappings").asJsonObject.get("url").asString
                            val clientURL = versionJson.get("downloads").asJsonObject.get("client").asJsonObject.get("url").asString
                            BufferedInputStream(URL(mappingsURL).openStream()).use { `in` ->
                                FileOutputStream(tempDir.absolutePath + File.separator + "mappings.txt").use { fileOutputStream ->
                                    val dataBuffer = ByteArray(1024)
                                    var bytesRead: Int
                                    while (`in`.read(dataBuffer, 0, 1024).also { bytesRead = it } != -1) {
                                        fileOutputStream.write(dataBuffer, 0, bytesRead)
                                    }
                                }
                            }
                            BufferedInputStream(URL(clientURL).openStream()).use { `in` ->
                                FileOutputStream(tempDir.absolutePath + File.separator + minecraftVersionBox.text + ".jar").use { fileOutputStream ->
                                    val dataBuffer = ByteArray(1024)
                                    var bytesRead: Int
                                    while (`in`.read(dataBuffer, 0, 1024).also { bytesRead = it } != -1) {
                                        fileOutputStream.write(dataBuffer, 0, bytesRead)
                                    }
                                }
                            }
                            val tree = MemoryMappingTree()
                            ProGuardFileReader.read(FileReader(tempDir.absolutePath + File.separator + "mappings.txt"), "mojmap", "notch", tree)
                            tree.accept(Tiny2FileWriter(OutputStreamWriter(FileOutputStream(tempDir.absolutePath + File.separator + "mappings.tiny")), false))
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
                progressLabel.text = "Downloading DracoLoader..."
                try {
                    BufferedInputStream(URL("https://jitpack.io/com/github/Draco-MC/DracoLoader/"+loaderVersionBox.text+"/DracoLoader-"+loaderVersionBox.text+"-all.jar").openStream()).use { `in` ->
                        FileOutputStream(tempDir.absolutePath + File.separator + "DracoLoader.jar").use { fileOutputStream ->
                            val dataBuffer = ByteArray(1024)
                            var bytesRead: Int
                            while (`in`.read(dataBuffer, 0, 1024).also { bytesRead = it } != -1) {
                                fileOutputStream.write(dataBuffer, 0, bytesRead)
                            }
                        }
                    }
                } catch (e: IOException) {
                    tempDir.deleteRecursively()
                    abortInstall(e.toString())
                    return@Thread
                }
                progressLabel.text = "Extracting DracoLoader Files..."
                try {
                    val dir = File(tempDir.absolutePath + File.separator + "DracoLoader")
                    dir.deleteRecursively()
                    dir.mkdir()
                    val pb = ProcessBuilder("jar", "xf", ".."+File.separator+"DracoLoader.jar")
                    pb.directory(dir)
                    if(pb.start().waitFor() != 0) {
                        tempDir.deleteRecursively()
                        abortInstall("Java SDK's \"jar\" exited with non-zero value!")
                        return@Thread
                    }
                } catch(e: Exception) {
                    tempDir.deleteRecursively()
                    abortInstall(e.toString())
                    return@Thread
                }
                progressLabel.text = "Adding DracoLoader Classes to Deobfuscated Minecraft JAR..."
                try {
                    val dir = File(tempDir.absolutePath + File.separator + "DracoLoader")
                    val pb = ProcessBuilder("jar", "uf", ".."+File.separator+minecraftVersionBox.text+"-deobf.jar", ".")
                    pb.directory(dir)
                    if(pb.start().waitFor() != 0) {
                        tempDir.deleteRecursively()
                        abortInstall("Java SDK's \"jar\" exited with non-zero value!")
                        return@Thread
                    }
                } catch(e: Exception) {
                    tempDir.deleteRecursively()
                    abortInstall(e.toString())
                    return@Thread
                }
                progressLabel.text = "Creating Minecraft Version Directory..."
                var ver = File(minecraftPath+File.separator+"versions"+File.separator+minecraftVersionBox.text+"-draco")
                ver.deleteRecursively()
                ver.mkdir()
                progressLabel.text = "Creating Manifest JSON..."
                var jsonObj = Gson().fromJson(tempDir.resolve("minecraft.json").readText(), JsonObject::class.java)
                jsonObj.remove("downloads")
                jsonObj.add("mainClass",JsonPrimitive("net.minecraft.launchwrapper.Launch"))
                val a = jsonObj.getAsJsonObject("arguments").getAsJsonArray("game")
                a.add("--tweakClass")
                a.add("sh.talonfloof.dracoloader.bootstrap.MinecraftClientBootstrap")
                jsonObj.add("id",JsonPrimitive(jsonObj.get("id").asString!!+"-draco"))
                ver.resolve(minecraftVersionBox.text+"-draco.json").writeText(Gson().toJson(jsonObj))
                progressLabel.text = "Moving Deobfuscated Minecraft JAR to Version Directory..."
                tempDir.resolve(minecraftVersionBox.text+"-deobf.jar").renameTo(ver.resolve(minecraftVersionBox.text+"-draco.jar"))
                progressLabel.text = "Cleaning up..."
                tempDir.deleteRecursively()
                progressLabel.text = "Installation Successful!"
                if (JOptionPane.showConfirmDialog(
                        this,
                        "Draco Loader has been successfully installed!\nWould you like to add a new profile for Draco?",
                        "Installation Successful!",
                        JOptionPane.YES_NO_OPTION
                    ) == JOptionPane.YES_OPTION
                ) {
                    jsonObj = Gson().fromJson(File(minecraftPath).resolve("launcher_profiles.json").readText(), JsonObject::class.java)
                    var profile = JsonObject()
                    profile.add("created",JsonPrimitive("1970-01-02T00:00:00.000Z"))
                    profile.add("lastUsed",JsonPrimitive("1970-01-02T00:00:00.000Z"))
                    profile.add("lastVersionId",JsonPrimitive(minecraftVersionBox.text+"-draco"))
                    profile.add("name",JsonPrimitive("Draco ("+minecraftVersionBox.text+")"))
                    profile.add("type",JsonPrimitive("custom"))
                    //profile.add("icon",JsonPrimitive("data:image/png;base64,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"))
                    jsonObj.getAsJsonObject("profiles").add(minecraftVersionBox.text+"-draco",profile)
                    File(minecraftPath).resolve("launcher_profiles.json").writeText(Gson().toJson(jsonObj))
                    JOptionPane.showMessageDialog(
                        this,
                        "Profile was successfully created\nIf the Minecraft Launcher is currently running then you'll need to restart it to see the new profile",
                        "Successfully Added Profile!",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                }
                progressBar.isIndeterminate = false
                installClientButton.isEnabled = true
                installServerButton.isEnabled = true
                exitButton.isEnabled = true
                minecraftVersionBox.isEnabled = true
                loaderVersionBox.isEnabled = true
                progressLabel.text = "To start, select a Draco version and a Minecraft version, then press install"
            }.start()
        }
    }

    private fun abortInstall(reason: String) {
        JOptionPane.showMessageDialog(this,reason,"Installation Failed!",JOptionPane.ERROR_MESSAGE)
        installClientButton.isEnabled = true
        installServerButton.isEnabled = true
        exitButton.isEnabled = true
        minecraftVersionBox.isEnabled = true
        loaderVersionBox.isEnabled = true
        progressBar.isIndeterminate = false
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