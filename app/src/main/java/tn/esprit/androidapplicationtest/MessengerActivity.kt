package tn.esprit.androidapplicationtest

import ChatAdapter
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import tn.esprit.androidapplicationtest.databinding.ActivityMessengerBinding
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL


data class Conversation2(
    val _id: String,
    val messages: List<Message2>,
    val attachments: List<Any>, // Modify this as needed
    val __v: Int
)

data class Message2(
    val _id: String,
    val sender: String,
    val conversation: String,
    val content: String,
    val timestamp: String,
    val __v: Int
)
data class Attachment(
    val _id: String,
    val conversation: String,
    val url: String,
    val __v: Int
)
interface MessageService {
    @GET("conversations/{conversationId}/messages")
    fun getMessages(@Path("conversationId") conversationId: String): Call<Conversation2>
}
interface AttachmentService {
    @GET("conversations/{conversationId}/attachments")
    fun getAttachments(@Path("conversationId") conversationId: String): Call<List<Attachment>>
}
class MessengerActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_PICK_FILE = 101
    }
    private lateinit var binding: ActivityMessengerBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var conversationId: String // Store the conversation ID
    private lateinit var socket: Socket
    private lateinit var currentUserId: String // Store the conversation ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessengerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        recyclerView = binding.recyclerView
        currentUserId = "65ca634c40ddbaf5e3db9d01"
        val opts = IO.Options()
        opts.forceNew = true
        socket = IO.socket("http://10.0.2.2:9090", opts)

        // Connect to the server
        socket.connect()
        socket.on(Socket.EVENT_CONNECT) {
            Log.d("MessengerActivity", "Socket connected")
            // Emit join_conversation event when socket connects
            socket.emit("join_conversation", conversationId)
        }.on(Socket.EVENT_DISCONNECT) {
            Log.d("MessengerActivity", "Socket disconnected")
        }


// Retrieve the conversation ID from intent or wherever you get it
        conversationId = intent.getStringExtra("conversationId") ?: ""
        var sendername =intent.getStringExtra("SENDERNAME") ?: ""
        if (conversationId.isBlank()) {
            // Handle the case where conversationId is not available
            return
        }

        var sendernameui =findViewById<TextView>(R.id.personname)
        val sendButton = findViewById<ImageView>(R.id.plus)
        val moreoption=findViewById<ImageView>(R.id.addd)
        val videoBtn = findViewById<ImageView>(R.id.videoBtn)
        val messageEditText = findViewById<EditText>(R.id.editTextUsername)

        moreoption.setOnClickListener{
            showMoreOptionDialog()
        }

//       // val attachmentButton = findViewById<ImageView>(R.id.voicemessage)
        val attachmentDisplayButton = findViewById<ImageView>(R.id.attachmentDisplayButton)
        sendernameui.text=sendername
        attachmentDisplayButton.setOnClickListener {
            displayAttachments()
        }
        videoBtn.setOnClickListener {

        }
// Set click listener for send button
        sendButton.setOnClickListener {
            val message = messageEditText.text.toString()
            if (message.isNotEmpty()) {
                val data = JSONObject()
                data.put("sender", currentUserId) // Change to your sender ID
                data.put("content", message)
                data.put("conversation", conversationId) // Use the conversation ID obtained earlier

                // Emit the message to the server
                socket.emit("new_message_$conversationId", data)

                // Clear the message text field
                messageEditText.text.clear()
                refreshMessages()
            }
        }





        /////////////////////////////////////////////
//        attachmentButton.setOnClickListener {
//            val intent = Intent(Intent.ACTION_GET_CONTENT)
//            intent.type = "*/*"  // Allow any file type
//            startActivityForResult(intent, REQUEST_PICK_FILE)
//        }
//////////////////////////////////////////////////////////

        // Initialize Retrofit and make network call
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:9090/") // Update with your server URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(MessageService::class.java)
        service.getMessages(conversationId).enqueue(object : Callback<Conversation2> {
            override fun onResponse(
                call: Call<Conversation2>,
                response: Response<Conversation2>
            ) {
                if (response.isSuccessful) {
                    val conversation = response.body()
                    conversation?.let {
                        Log.d("MessengerActivity", "Number of messages received: ${conversation.messages.size}")
                        displayMessages(conversation.messages)
                    }
                } else {
                    Log.e("MessengerActivity", "Failed to fetch messages: ${response.code()}")
                    Toast.makeText(this@MessengerActivity, "Failed to fetch messages", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Conversation2>, t: Throwable) {
                Log.e("MessengerActivity", "Network Error: ${t.message}")
                Toast.makeText(this@MessengerActivity, "Network Error", Toast.LENGTH_SHORT).show()
            }
        })


    }
    private fun sendAttachment(filePath: String, conversationId: String) {
        if (File(filePath).exists()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val fileName = File(filePath).name
                    val url = URL("http://10.0.2.2:9090/conversations/$conversationId/attachments")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=*****")

                    val outputStream = DataOutputStream(connection.outputStream)
                    val lineEnd = "\r\n"
                    val twoHyphens = "--"
                    val boundary = "*****"

                    outputStream.writeBytes("$twoHyphens$boundary$lineEnd")
                    outputStream.writeBytes("Content-Disposition: form-data; name=\"file\";filename=\"$fileName\"$lineEnd")
                    outputStream.writeBytes(lineEnd)

                    val fileInputStream = FileInputStream(filePath)
                    var bytesAvailable = fileInputStream.available()
                    var bufferSize = Math.min(bytesAvailable, 1024)
                    val buffer = ByteArray(bufferSize)
                    var bytesRead = fileInputStream.read(buffer, 0, bufferSize)

                    while (bytesRead > 0) {
                        outputStream.write(buffer, 0, bufferSize)
                        bytesAvailable = fileInputStream.available()
                        bufferSize = Math.min(bytesAvailable, 1024)
                        bytesRead = fileInputStream.read(buffer, 0, bufferSize)
                    }

                    outputStream.writeBytes(lineEnd)
                    outputStream.writeBytes("$twoHyphens$boundary$twoHyphens$lineEnd")

                    // Close streams
                    fileInputStream.close()
                    outputStream.flush()
                    outputStream.close()

                    val responseCode = connection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_CREATED) {
                        // Attachment sent successfully
                        Log.d("MessengerActivity", "Attachment sent successfully")
                    } else {
                        // Error sending attachment
                        Log.e("MessengerActivity", "Failed to send attachment. Response code: $responseCode")
                    }
                    connection.disconnect()
                } catch (e: Exception) {
                    // Handle any errors that occur during the HTTP request
                    Log.e("MessengerActivity", "Error sending attachment: ${e.message}")
                }
            }
        } else {
            Log.e("MessengerActivity", "File does not exist at path: $filePath")
        }
    }
    private fun showMoreOptionDialog() {
        val dialog = BottomSheetDialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.moreoptions)
        var attachbutton = dialog.findViewById<ImageView>(R.id.attach)
        attachbutton?.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"  // Allow any file type
            startActivityForResult(intent, REQUEST_PICK_FILE)
        }
        dialog.show()
    }
    private fun refreshMessages() {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:9090/") // Update with your server URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        // Fetch the messages from the server again and update the RecyclerView
        val service = retrofit.create(MessageService::class.java)
        service.getMessages(conversationId).enqueue(object : Callback<Conversation2> {
            override fun onResponse(
                call: Call<Conversation2>,
                response: Response<Conversation2>
            ) {
                if (response.isSuccessful) {
                    val conversation = response.body()
                    conversation?.let {
                        Log.d("MessengerActivity", "Number of messages received: ${conversation.messages.size}")
                        displayMessages(conversation.messages)
                    }
                } else {
                    Log.e("MessengerActivity", "Failed to fetch messages: ${response.code()}")
                    Toast.makeText(this@MessengerActivity, "Failed to fetch messages", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Conversation2>, t: Throwable) {
                Log.e("MessengerActivity", "Network Error: ${t.message}")
                Toast.makeText(this@MessengerActivity, "Network Error", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun displayMessages(messages: List<Message2>) {
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        // Inside MessengerActivity onCreate method after getting conversationId
        val adapter = ChatAdapter(messages, currentUserId)
        recyclerView.adapter = adapter

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_FILE && resultCode == Activity.RESULT_OK) {
            val selectedFileUri: Uri? = data?.data
            selectedFileUri?.let {
                val filePath = getPathFromUri(selectedFileUri) // Get the file path from URI
                sendAttachment(filePath, conversationId)
            }
        }
    }
    private fun getPathFromUri(uri: Uri): String {
        val contentResolver = applicationContext.contentResolver
        var filePath = ""
        try {
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val file = File.createTempFile("temp", null, applicationContext.cacheDir)
                val outputStream = FileOutputStream(file)
                inputStream.copyTo(outputStream)
                filePath = file.absolutePath
                inputStream.close()
                outputStream.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return filePath
    }
    private fun displayAttachments() {
        // Create Retrofit instance
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:9090/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // Create service interface
        val service = retrofit.create(AttachmentService::class.java)

        // Make network request to get attachments
        service.getAttachments(conversationId).enqueue(object : Callback<List<Attachment>> {
            override fun onResponse(call: Call<List<Attachment>>, response: Response<List<Attachment>>) {
                if (response.isSuccessful) {
                    val attachments = response.body()
                    attachments?.let { displayImages(it) }
                } else {
                    Log.e("MessengerActivity", "Failed to fetch attachments: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<Attachment>>, t: Throwable) {
                Log.e("MessengerActivity", "Error fetching attachments: ${t.message}")
            }
        })
    }

    private fun displayImages(attachments: List<Attachment>) {
        // Use an AlertDialog to display the attachments
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Attachments")

        // Create a ScrollView to handle scrolling if there are too many images
        val scrollView = ScrollView(this)
        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL

        // Set layout parameters for the ImageView to wrap content
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        // Load and add each attachment image to the container
        attachments.forEach { attachment ->
            val imageView = ImageView(this)
            imageView.layoutParams = params // Apply layout parameters
            Glide.with(this)
                .load(attachment.url)
                .into(imageView)
            container.addView(imageView)
        }

        scrollView.addView(container)
        dialogBuilder.setView(scrollView)
        dialogBuilder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        dialogBuilder.show()
    }
}


