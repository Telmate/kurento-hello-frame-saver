/*
 * (C) Copyright 2014 Kurento (http://mClientKurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.kurento.tutorial.frame_saver;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import org.kurento.client.AudioCaps;
import org.kurento.client.Continuation;
import org.kurento.client.ElementConnectedEvent;
import org.kurento.client.ElementConnectionData;
import org.kurento.client.ElementDisconnectedEvent;
import org.kurento.client.ErrorEvent;
import org.kurento.client.EventListener;
import org.kurento.client.GstreamerDotDetails;
import org.kurento.client.IceCandidate;
import org.kurento.client.IceCandidateFoundEvent;
import org.kurento.client.KurentoClient;
import org.kurento.client.ListenerSubscription;
import org.kurento.client.MediaElement;
import org.kurento.client.MediaFlowInStateChangeEvent;
import org.kurento.client.MediaFlowOutStateChangeEvent;
import org.kurento.client.MediaObject;
import org.kurento.client.MediaPipeline;
import org.kurento.client.MediaType;
import org.kurento.client.PausedEvent;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.client.Stats;
import org.kurento.client.StoppedEvent;
import org.kurento.client.TFuture;
import org.kurento.client.Tag;
import org.kurento.client.Transaction;
import org.kurento.client.UriEndpointState;
import org.kurento.client.UriEndpointStateChangedEvent;
import org.kurento.client.VideoCaps;
import org.kurento.jsonrpc.JsonUtils;

import com.kurento.kmf.media.AbstractBuilder;
import org.kurento.tutorial.frame_saver.FrameSaverFilter.Builder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * FrameSaverHandler (application and media logic).
 *
 * @author J. Bendor for Telmate Inc. --- based on "kurento-plate-detector"
 */
public class FrameSaverHandler extends TextWebSocketHandler
{    
    private static final Gson s_Gson = new GsonBuilder().create();

    private final Logger mLogger = LoggerFactory.getLogger(FrameSaverHandler.class);

    private final ConcurrentHashMap<String, UserSession> mSessionsMap = new ConcurrentHashMap<>();

    @Autowired
    private KurentoClient mClientKurento;

    @Override
    public void handleTextMessage(WebSocketSession aSession, TextMessage aMessage) throws Exception
    {
        JsonObject json_obj_msg = s_Gson.fromJson(aMessage.getPayload(), JsonObject.class);

        mLogger.debug("Incoming message: {}", json_obj_msg);

        String msg_id = json_obj_msg.get("id").getAsString();

        switch (msg_id)
        {
        case "start":
            start(aSession, json_obj_msg);
            break;

        case "stop":
        {
            UserSession user_session = mSessionsMap.remove(aSession.getId());

            if (user_session != null)
            {
                user_session.release();
            }
            break;
        }

        case "onIceCandidate":
        {
            UserSession user_session = mSessionsMap.get(aSession.getId());

            if (user_session != null)
            {
                JsonObject old_candidate = json_obj_msg.get("candidate").getAsJsonObject();

                IceCandidate new_candidate = new IceCandidate(old_candidate.get("candidate").getAsString(),
                                                              old_candidate.get("sdpMid").getAsString(),
                                                              old_candidate.get("sdpMLineIndex").getAsInt());
                user_session.addCandidate(new_candidate);
            }
            break;
        }

        default:
            sendError(aSession, "Invalid JSON message id: " + msg_id);
            break;
        }
    }

    private void start(final WebSocketSession aSession, JsonObject aJsonMsg)
    {
        try
        {
            MediaPipeline new_pipeline = mClientKurento.createMediaPipeline();

            WebRtcEndpoint new_endpoint = new WebRtcEndpoint.Builder(new_pipeline).build();

            UserSession new_session = new UserSession();

            new_session.setMediaPipeline(new_pipeline);

            new_session.setWebRtcEndpoint(new_endpoint);

            mSessionsMap.put(aSession.getId(), new_session);

            new_endpoint.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>()
            {
                @Override
                public void onEvent(IceCandidateFoundEvent event)
                {
                    JsonObject response_obj = new JsonObject();

                    response_obj.addProperty("id", "iceCandidate");

                    response_obj.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));

                    String response_str = response_obj.toString();

                    try
                    {
                        synchronized (aSession)
                        {
                            aSession.sendMessage(new TextMessage(response_str));
                        }
                    }
                    catch (IOException ex)
                    {
                        mLogger.debug(ex.getMessage());
                    }
                }
            });

            FrameSaverFilter frame_saver_filter = new FrameSaverFilter.Builder(new_pipeline).build();

            new_endpoint.connect(frame_saver_filter);

            frame_saver_filter.connect(new_endpoint);

            frame_saver_filter.addFrameSaverListener(new EventListener<FrameSaverEvent>()
            {
                @Override
                public void onEvent(FrameSaverEvent event)
                {
                    JsonObject response_obj = new JsonObject();

                    response_obj.addProperty("id", "TODO");

                    String response_str = response_obj.toString();

                    try
                    {
                        aSession.sendMessage(new TextMessage(response_str));
                    }
                    catch (Throwable ex)
                    {
                        sendError(aSession, ex.getMessage());
                    }
                }
            });

            // SDP negotiation (offer and answer)
            String sdp_offer = aJsonMsg.get("sdpOffer").getAsString();

            String sdp_reply = new_endpoint.processOffer(sdp_offer);

            // Sending response back to client
            JsonObject response_obj = new JsonObject();
            response_obj.addProperty("id", "startResponse");
            response_obj.addProperty("sdpAnswer", sdp_reply);

            String response_str = response_obj.toString();

            synchronized (aSession)
            {
                aSession.sendMessage(new TextMessage(response_str));
            }

            new_endpoint.gatherCandidates();
        }
        catch (Throwable ex)
        {
            sendError(aSession, ex.getMessage());
        }
    }

    private void sendError(WebSocketSession aSession, String aMessage)
    {
        try
        {
            JsonObject response_obj = new JsonObject();

            response_obj.addProperty("id", "error");

            response_obj.addProperty("message", aMessage);

            String response_str = response_obj.toString();

            aSession.sendMessage(new TextMessage(response_str));
        }
        catch (IOException ex)
        {
            mLogger.error("Exception sending message", ex);
        }
    }
}
