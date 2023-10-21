// Generated by view binder compiler. Do not edit!
package de.yanneckreiss.cameraxtutorial.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import de.yanneckreiss.cameraxtutorial.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class ActivityChatRoomBinding implements ViewBinding {
  @NonNull
  private final RelativeLayout rootView;

  @NonNull
  public final RelativeLayout bottomLayout;

  @NonNull
  public final Button button;

  @NonNull
  public final EditText messageTextText;

  @NonNull
  public final RecyclerView recyclerView;

  @NonNull
  public final ImageView sendBtn;

  private ActivityChatRoomBinding(@NonNull RelativeLayout rootView,
      @NonNull RelativeLayout bottomLayout, @NonNull Button button,
      @NonNull EditText messageTextText, @NonNull RecyclerView recyclerView,
      @NonNull ImageView sendBtn) {
    this.rootView = rootView;
    this.bottomLayout = bottomLayout;
    this.button = button;
    this.messageTextText = messageTextText;
    this.recyclerView = recyclerView;
    this.sendBtn = sendBtn;
  }

  @Override
  @NonNull
  public RelativeLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static ActivityChatRoomBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static ActivityChatRoomBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.activity_chat_room, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static ActivityChatRoomBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.bottom_layout;
      RelativeLayout bottomLayout = ViewBindings.findChildViewById(rootView, id);
      if (bottomLayout == null) {
        break missingId;
      }

      id = R.id.button;
      Button button = ViewBindings.findChildViewById(rootView, id);
      if (button == null) {
        break missingId;
      }

      id = R.id.message_text_text;
      EditText messageTextText = ViewBindings.findChildViewById(rootView, id);
      if (messageTextText == null) {
        break missingId;
      }

      id = R.id.recyclerView;
      RecyclerView recyclerView = ViewBindings.findChildViewById(rootView, id);
      if (recyclerView == null) {
        break missingId;
      }

      id = R.id.send_btn;
      ImageView sendBtn = ViewBindings.findChildViewById(rootView, id);
      if (sendBtn == null) {
        break missingId;
      }

      return new ActivityChatRoomBinding((RelativeLayout) rootView, bottomLayout, button,
          messageTextText, recyclerView, sendBtn);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}