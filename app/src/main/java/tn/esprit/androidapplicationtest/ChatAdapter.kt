
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView
import tn.esprit.androidapplicationtest.Message2
import tn.esprit.androidapplicationtest.R

class ChatAdapter(private val messages: List<Message2>, private val currentUserId: String) :
    RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    private val VIEW_TYPE_SENT = 1
    private val VIEW_TYPE_RECEIVED = 2

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Views for the left side layout (received messages)
        val messageText: TextView = view.findViewById(R.id.textMessage)
        val timeText: TextView = view.findViewById(R.id.heure)
        val hide1:  TextView = view.findViewById(R.id.espace1)
        val hide2:  TextView = view.findViewById(R.id.espace2)
        val phote: ShapeableImageView = view.findViewById(R.id.photo)
        val cart: MaterialCardView =view.findViewById(R.id.con)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.messageitem, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = messages[position]
        holder.messageText.text = message.content
        holder.timeText.text = message.timestamp

        if (message.sender == currentUserId) {
            // Message sent by the current user
            holder.hide1.visibility=View.GONE
            holder.hide2.visibility=View.GONE
            holder.phote.visibility=View.GONE
            holder.cart.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.softred))
        }else
        {
            holder.hide1.visibility=View.VISIBLE
            holder.hide2.visibility=View.VISIBLE
            holder.phote.visibility=View.VISIBLE
            holder.cart.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.msgC))

        }
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return if (message.sender == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }
}
