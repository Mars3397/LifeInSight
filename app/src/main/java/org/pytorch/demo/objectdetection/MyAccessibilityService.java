package org.pytorch.demo.objectdetection;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.ImageView;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class MyAccessibilityService extends AccessibilityService {

    private Button REC_btn;
    private ImageView icon;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode != null) {
            printNodeInfo(rootNode);
        }
        int eventType = event.getEventType();
        AccessibilityNodeInfo sourceNode = event.getSource();

        if (sourceNode == null) {
            return;
        }


    }

    private void printNodeInfo(AccessibilityNodeInfo node) {
        if (node == null) {
            return;
        }

        Log.d("AccessibilityService", "Node: " + node.toString());

        // 打印节点的子节点信息
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo childNode = node.getChild(i);
            printNodeInfo(childNode);
        }
    }


    @Override
    public void onInterrupt() {

    }

    public String decideNext(String currentViewId) {
        switch (currentViewId) {
            case "org.pytorch.demo.objectdetection:id/recButton":
                return "org.pytorch.demo.objectdetection:id/rightIcon";
            case "org.pytorch.demo.objectdetection:id/rightIcon":
                return "org.pytorch.demo.objectdetection:id/recButton";
            default:
                return "null";
        }
    }
    private TextToSpeech tts;
    public void speak(String text) {
        tts = new TextToSpeech(getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                Locale chineseLocale = new Locale("zh", "CN");
                tts.setLanguage(chineseLocale);
                tts.setSpeechRate(1.5f);
                tts.speak(text, TextToSpeech.QUEUE_ADD, null, null);
            }
        });
    }

    private AccessibilityNodeInfo findButtonNodeById(AccessibilityNodeInfo nodeInfo, String buttonId) {
        LinkedList<AccessibilityNodeInfo> queue = new LinkedList<>();
        queue.add(nodeInfo);

        while (!queue.isEmpty()) {
            AccessibilityNodeInfo currentNode = queue.poll();
            if (buttonId.equals(currentNode.getViewIdResourceName())) {
                return currentNode;
            }

            for (int i = 0; i < currentNode.getChildCount(); i++) {
                queue.add(currentNode.getChild(i));
            }
        }
        return null;
    }

    String curFocus = "org.pytorch.demo.objectdetection:id/recButton";
    String nextFocus = "org.pytorch.demo.objectdetection:id/rightIcon";

    public void read(){
        if(nextFocus == "org.pytorch.demo.objectdetection:id/recButton"){
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            List<AccessibilityNodeInfo> buttonNodes = rootNode.findAccessibilityNodeInfosByViewId("org.pytorch.demo.objectdetection:id/recButton");
            if (buttonNodes != null && !buttonNodes.isEmpty()) {
                AccessibilityNodeInfo buttonNode = buttonNodes.get(0);
                CharSequence buttonText = buttonNode.getText();
                if (buttonText != null) {
                    String buttonTextString = buttonText.toString();
                    speak(buttonTextString);
                }
            }
        }
        else{
            speak("尋找文字");
        }
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        Log.d("AccessibilityNodeInfo", "service begin");

        IntentFilter doubleClickFilter = new IntentFilter("org.pytorch.demo.objectdetection.DOUBLE_CLICKED");
        IntentFilter singleClickFilter = new IntentFilter("org.pytorch.demo.objectdetection.SINGLE_CLICKED");
        IntentFilter longPressedFilter = new IntentFilter("org.pytorch.demo.objectdetection.LONG_PRESSED");
        IntentFilter rightSwipeFilter = new IntentFilter("org.pytorch.demo.objectdetection.RIGHT_SWIPE");
        IntentFilter leftSwipeFilter = new IntentFilter("org.pytorch.demo.objectdetection.LEFT_SWIPE");


        final BroadcastReceiver customReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null || intent.getAction() == null) {
                    return;
                }

                switch (intent.getAction()) {
                    case "org.pytorch.demo.objectdetection.DOUBLE_CLICKED": {
                        Log.d("screen_action", "Received DOUBLE_CLICKED Intent");

                        AccessibilityNodeInfo rootNode = getRootInActiveWindow();

                        String curFocusId = curFocus; // Assuming that 'curFocus' is a member variable.
                        List<AccessibilityNodeInfo> curButtonNodes = rootNode.findAccessibilityNodeInfosByViewId(curFocusId);

                        if (!curButtonNodes.isEmpty()) {
                            AccessibilityNodeInfo curNode = curButtonNodes.get(0);
                            boolean focusSuccess = curNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS);

                            String viewIdResourceName = rootNode.getViewIdResourceName();

                            if (focusSuccess) {
                                Log.d("Focus!", "焦点成功设置在 " + curFocus + " 上");
                                boolean clickSuccess = curNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);



                            } else {
                                Log.d("Focus!", "焦点设置失败");
                            }
                        } else {
                            Log.d("..", "fail");
                        }
                        break;
                    }

                    case "org.pytorch.demo.objectdetection.SINGLE_CLICKED": {
                        Log.d("screen_action", "Received SINGLE_CLICKED Intent");

                        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                        nextFocus = decideNext(curFocus);


                        Log.d("Focus!", "single click, cur = " + curFocus + ", next = " + nextFocus);
                        AccessibilityNodeInfo focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);

                        List<AccessibilityNodeInfo> permitButtonNodes = rootNode.findAccessibilityNodeInfosByViewId(nextFocus);
                        if (!permitButtonNodes.isEmpty()) {
                            AccessibilityNodeInfo permitNode = permitButtonNodes.get(0);
                            boolean focusSuccess = permitNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS);

                            String viewIdResourceName = rootNode.getViewIdResourceName();

                            if (focusSuccess) {
                                Log.d("Focus!", "click焦点成功设置在 " + nextFocus + " 上");
                                read();
                                curFocus = nextFocus;
                            } else {
                                Log.d("Focus!", "click焦点设置失败");
                            }
                        } else {
                            Log.d(".....", "fail");

                            break;
                        }

                    }

                    case "org.pytorch.demo.objectdetection.RIGHT_SWIPE": {
                        AccessibilityNodeInfo rootNode = getRootInActiveWindow();

                        Log.d("screen_action", "Received RIGHT_SWIPE Intent");
                        nextFocus = decideNext(curFocus);
                        Log.d("Focus!", "right swipe, cur = " + curFocus + ", next = " + nextFocus);
                        List<AccessibilityNodeInfo> permitButtonNodes = rootNode.findAccessibilityNodeInfosByViewId(nextFocus);
                        if (!permitButtonNodes.isEmpty()) {
                            AccessibilityNodeInfo permitNode = permitButtonNodes.get(0);
                            boolean focusSuccess = permitNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS);

                            String viewIdResourceName = rootNode.getViewIdResourceName();

                            if (focusSuccess) {
                                Log.d("Focus!", "click焦点成功设置在 " + nextFocus + " 上");
                                read();
                                curFocus = nextFocus;
                            } else {
                                Log.d("....", "fail");
                            }
                        }
                        break;
                    }

                    case "org.pytorch.demo.objectdetection.LEFT_SWIPE": {
                        AccessibilityNodeInfo rootNode = getRootInActiveWindow();

                        Log.d("screen_action", "Received LEFT_SWIPE Intent");
                        nextFocus = decideNext(curFocus);
                        Log.d("Focus!", "left swipe, cur = " + curFocus + ", next = " + nextFocus);

                        List<AccessibilityNodeInfo> permitButtonNodes = rootNode.findAccessibilityNodeInfosByViewId(nextFocus);
                        if (!permitButtonNodes.isEmpty()) {
                            AccessibilityNodeInfo permitNode = permitButtonNodes.get(0);
                            boolean focusSuccess = permitNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS);

                            String viewIdResourceName = rootNode.getViewIdResourceName();

                            if (focusSuccess) {
                                Log.d("Focus!", "click焦点成功设置在 " + nextFocus + " 上");
                                read();
                                curFocus = nextFocus;
                            } else {
                                Log.d("....", "fail");
                            }
                        }
                        break;
                    }

                    default:
                        Log.d("Focus!", "click fail");
                        break;


                }
                ;



            }
        };
        registerReceiver(customReceiver, doubleClickFilter);
        registerReceiver(customReceiver, singleClickFilter);
        registerReceiver(customReceiver, longPressedFilter);
        registerReceiver(customReceiver, leftSwipeFilter);
        registerReceiver(customReceiver, rightSwipeFilter);
    }
}