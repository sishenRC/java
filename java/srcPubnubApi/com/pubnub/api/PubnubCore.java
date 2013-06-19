package com.pubnub.api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Random;

import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Pubnub object facilitates querying channels for messages and listening on
 * channels for presence/message events
 *
 * @author Pubnub
 *
 */

abstract class PubnubCore {

    private String HOSTNAME = "pubsub";
    private int HOSTNAME_SUFFIX = 1;
    private String DOMAIN = "pubnub.com";
    private String ORIGIN_STR = null;
    private String PUBLISH_KEY = "";
    private String SUBSCRIBE_KEY = "";
    private String SECRET_KEY = "";
    private String CIPHER_KEY = "";
    private volatile String AUTH_STR = null;
    private Hashtable params;
    private volatile boolean resumeOnReconnect;

    private boolean SSL = true;
    protected String UUID = null;
    private Subscriptions subscriptions;

    protected SubscribeManager subscribeManager;
    protected NonSubscribeManager nonSubscribeManager;
    private volatile String _timetoken = "0";
    private volatile String _saved_timetoken = "0";

    private String PRESENCE_SUFFIX = "-pnpres";
    private static String VERSION = "";
    private Random generator = new Random();

    private static Logger log = new Logger(PubnubCore.class);

    protected abstract String getUserAgent();

    /**
     * This method when called stops Pubnub threads
     */
    public void shutdown() {
        nonSubscribeManager.stop();
        subscribeManager.stop();
    }

    /**
     * This method returns the state of Resume on Reconnect setting
     *
     * @return Current state of Resume On Reconnect Setting
     */
    public boolean isResumeOnReconnect() {
        return resumeOnReconnect;
    }

    /**
     * This method sets retry interval for subscribe. Pubnub API will make
     * maxRetries attempts to connect to pubnub servers. These attemtps will be
     * made at an interval of retryInterval milliseconds.
     *
     * @param retryInterval
     *            Retry Interval in milliseconds
     */
    public void setRetryInterval(int retryInterval) {
        subscribeManager.setRetryInterval(retryInterval);
    }

    /**
     * This methods sets maximum number of retries for subscribe. Pubnub API
     * will make maxRetries attempts to connect to pubnub servers before timing
     * out.
     *
     * @param maxRetries
     *            Max number of retries
     */
    public void setMaxRetries(int maxRetries) {
        subscribeManager.setMaxRetries(maxRetries);
    }

    private String getOrigin() {
        if (ORIGIN_STR == null) {
            // SSL On?
            if (this.SSL) {
                ORIGIN_STR = "https://" + HOSTNAME + "-"
                        + String.valueOf(HOSTNAME_SUFFIX) + "." + DOMAIN;
            } else {
                ORIGIN_STR = "http://" + HOSTNAME + "-"
                        + String.valueOf(HOSTNAME_SUFFIX) + "." + DOMAIN;
            }
        }
        return ORIGIN_STR;
    }

    private Hashtable hashtableClone(Hashtable ht) {
        if (ht == null)
            return null;

        Hashtable htresp = new Hashtable();
        Enumeration e = ht.keys();

        while (e.hasMoreElements()) {
            Object element = e.nextElement();
            htresp.put(element, ht.get(element));
        }
        return htresp;
    }

    /**
     * This method returns all channel names currently subscribed to in form of
     * a comma separated String
     *
     * @return Comma separated string with all channel names currently
     *         subscribed
     */
    public String getCurrentlySubscribedChannelNames() {
        String currentChannels = subscriptions.getChannelString();
        return currentChannels.equals("") ? "no channels." : currentChannels;
    }

    /**
     * If Resume on Reconnect is set to true, then Pubnub catches up on
     * reconnection after disconnection. If false, then messages sent on the
     * channel between disconnection and reconnection are not received.
     *
     * @param resumeOnReconnect
     *            True or False setting for Resume on Reconnect
     */
    public void setResumeOnReconnect(boolean resumeOnReconnect) {
        this.resumeOnReconnect = resumeOnReconnect;
    }

    /**
     * Convert input String to JSONObject, JSONArray, or String
     *
     * @param str
     *            JSON data in string format
     *
     * @return JSONArray or JSONObject or String
     */
    static Object stringToJSON(String str) {
        Object obj = str;
        try {
            JSONArray jsarr = new JSONArray(str);
            obj = jsarr;
        } catch (JSONException e) {
            try {
                JSONObject jsobj = new JSONObject(str);
                obj = jsobj;
            } catch (JSONException ex) {
            }
        }
        return obj;
    }

    protected abstract String uuid();

    /**
     * Sets value for UUID
     *
     * @param uuid
     *            UUID value for Pubnub client
     */
    public void setUUID(String uuid) {
        this.UUID = uuid;
    }

    /**
     *
     * Constructor for Pubnub Class
     *
     * @param publish_key
     *            Publish Key
     * @param subscribe_key
     *            Subscribe Key
     * @param secret_key
     *            Secret Key
     * @param cipher_key
     *            Cipher Key
     * @param ssl_on
     *            SSL enabled ?
     */

    public PubnubCore(String publish_key, String subscribe_key,
            String secret_key, String cipher_key, boolean ssl_on) {
        this.init(publish_key, subscribe_key, secret_key, cipher_key, ssl_on);
    }

    /**
     *
     * Constructor for Pubnub Class
     *
     * @param publish_key
     *            Publish Key
     * @param subscribe_key
     *            Subscribe Key
     * @param secret_key
     *            Secret Key
     * @param ssl_on
     *            SSL enabled ?
     */

    public PubnubCore(String publish_key, String subscribe_key,
            String secret_key, boolean ssl_on) {
        this.init(publish_key, subscribe_key, secret_key, "", ssl_on);
    }

    /**
     *
     * Constructor for Pubnub Class
     *
     * @param publish_key
     *            Publish Key
     * @param subscribe_key
     *            Subscribe Key
     */

    public PubnubCore(String publish_key, String subscribe_key) {
        this.init(publish_key, subscribe_key, "", "", false);
    }

    /**
     *
     * Constructor for Pubnub Class
     *
     * @param publish_key
     *            Publish Key
     * @param subscribe_key
     *            Subscribe Key
     */

    public PubnubCore(String publish_key, String subscribe_key, boolean ssl) {
        this.init(publish_key, subscribe_key, "", "", ssl);
    }

    /**
     *
     * Constructor for Pubnub Class
     *
     * @param publish_key
     *            Publish Key
     * @param subscribe_key
     *            Subscribe Key
     * @param secret_key
     *            Secret Key
     */
    public PubnubCore(String publish_key, String subscribe_key,
            String secret_key) {
        this.init(publish_key, subscribe_key, secret_key, "", false);
    }

    /**
     *
     * Initialize PubNub Object State.
     *
     * @param publish_key
     * @param subscribe_key
     * @param secret_key
     * @param cipher_key
     * @param ssl_on
     */
    private void init(String publish_key, String subscribe_key,
            String secret_key, String cipher_key, boolean ssl_on) {
        this.PUBLISH_KEY = publish_key;
        this.SUBSCRIBE_KEY = subscribe_key;
        this.SECRET_KEY = secret_key;
        this.CIPHER_KEY = cipher_key;
        this.SSL = ssl_on;

        if (UUID == null)
            UUID = uuid();

        if (subscriptions == null)
            subscriptions = new Subscriptions();

        if (subscribeManager == null)
            subscribeManager = new SubscribeManager("Subscribe-Manager-"
                    + System.identityHashCode(this), 10000, 310000);

        if (nonSubscribeManager == null)
            nonSubscribeManager = new NonSubscribeManager(
                    "Non-Subscribe-Manager-" + System.identityHashCode(this),
                    10000, 15000);

        if (params == null)
            params = new Hashtable();

        subscribeManager.setHeader("V", VERSION);
        subscribeManager.setHeader("Accept-Encoding", "gzip");
        subscribeManager.setHeader("User-Agent", getUserAgent());

        nonSubscribeManager.setHeader("V", VERSION);
        nonSubscribeManager.setHeader("Accept-Encoding", "gzip");
        nonSubscribeManager.setHeader("User-Agent", getUserAgent());

    }

    /**
     * This method sets timeout value for subscribe/presence. Default value is
     * 310000 milliseconds i.e. 310 seconds
     *
     * @param timeout
     *            Timeout value in milliseconds for subscribe/presence
     */
    protected void setSubscribeTimeout(int timeout) {
        subscribeManager.setRequestTimeout(timeout);
        this.disconnectAndResubscribe();
    }

    /**
     * This method set timeout value for non subscribe operations like publish,
     * history, hereNow. Default value is 15000 milliseconds i.e. 15 seconds.
     *
     * @param timeout
     *            Timeout value in milliseconds for Non subscribe operations
     *            like publish, history, hereNow
     */
    protected void setNonSubscribeTimeout(int timeout) {
        nonSubscribeManager.setRequestTimeout(timeout);
    }

    /**
     * Send a message to a channel.
     *
     * @param channel
     *            Channel name
     * @param message
     *            JSONObject to be published
     * @param callback
     *            object of sub class of Callback class
     */
    public void publish(String channel, JSONObject message, Callback callback) {
        Hashtable args = new Hashtable();
        args.put("channel", channel);
        args.put("message", message);
        args.put("callback", callback);
        publish(args);
    }

    /**
     * Send a message to a channel.
     *
     * @param channel
     *            Channel name
     * @param message
     *            JSONOArray to be published
     * @param callback
     *            object of sub class of Callback class
     */
    public void publish(String channel, JSONArray message, Callback callback) {
        Hashtable args = new Hashtable();
        args.put("channel", channel);
        args.put("message", message);
        args.put("callback", callback);
        publish(args);
    }

    /**
     * Send a message to a channel.
     *
     * @param channel
     *            Channel name
     * @param message
     *            String to be published
     * @param callback
     *            object of sub class of Callback class
     */
    public void publish(String channel, String message, Callback callback) {
        Hashtable args = new Hashtable();
        args.put("channel", channel);
        args.put("message", message);
        args.put("callback", callback);
        publish(args);
    }

    /**
     * Send a message to a channel.
     *
     * @param channel
     *            Channel name
     * @param message
     *            Integer to be published
     * @param callback
     *            object of sub class of Callback class
     */
    public void publish(String channel, Integer message, Callback callback) {
        Hashtable args = new Hashtable();
        args.put("channel", channel);
        args.put("message", message);
        args.put("callback", callback);
        publish(args);
    }

    /**
     * Send a message to a channel.
     *
     * @param channel
     *            Channel name
     * @param message
     *            Double to be published
     * @param callback
     *            object of sub class of Callback class
     */
    public void publish(String channel, Double message, Callback callback) {
        Hashtable args = new Hashtable();
        args.put("channel", channel);
        args.put("message", message);
        args.put("callback", callback);
        publish(args);
    }

    /**
     * Send a message to a channel.
     *
     * @param args
     *            Hashtable containing channel name, message.
     * @param callback
     *            object of sub class of Callback class
     */
    public void publish(Hashtable args, Callback callback) {
        args.put("callback", callback);
        publish(args);
    }

    /**
     * Send a message to a channel.
     *
     * @param args
     *            Hashtable containing channel name, message, callback
     */

    public void publish(Hashtable args) {

        final String channel = (String) args.get("channel");
        final Object message = args.get("message");
        final Callback callback = (Callback) args.get("callback");
        String msgStr = message.toString();

        if (this.CIPHER_KEY.length() > 0) {
            // Encrypt Message
            PubnubCrypto pc = new PubnubCrypto(this.CIPHER_KEY);
            try {
                msgStr = "\"" + pc.encrypt(msgStr) + "\"";
            }
            catch (DataLengthException e) {
                callback.errorCallback(channel,
                        PubnubError.PNERROBJ_5033_ENCRYPTION_ERROR);
                return;
            }
            catch (IllegalStateException e) {
                callback.errorCallback(channel,
                        PubnubError.PNERROBJ_5034_ENCRYPTION_ERROR);
                return;
            }
            catch (InvalidCipherTextException e) {
                callback.errorCallback(channel,
                        PubnubError.PNERROBJ_5035_ENCRYPTION_ERROR);
                return;
            }
        } else {
            if (message instanceof String) {
                msgStr = "\"" + msgStr + "\"";
            }
        }

        // Generate String to Sign
        String signature = "0";

        if (this.SECRET_KEY.length() > 0) {
            StringBuffer string_to_sign = new StringBuffer();
            string_to_sign.append(this.PUBLISH_KEY).append('/')
            .append(this.SUBSCRIBE_KEY).append('/')
            .append(this.SECRET_KEY).append('/').append(channel)
            .append('/').append(msgStr);

            // Sign Message
            try {
                signature = new String(Hex.encode(PubnubCrypto
                        .md5(string_to_sign.toString())), "UTF-8");
            } catch (UnsupportedEncodingException e) {

            }
        }
        String[] urlComponents = { getOrigin(), "publish", this.PUBLISH_KEY,
                this.SUBSCRIBE_KEY, PubnubUtil.urlEncode(signature),
                PubnubUtil.urlEncode(channel), "0",
                PubnubUtil.urlEncode(msgStr) };

        HttpRequest hreq = new HttpRequest(urlComponents, params,
                new ResponseHandler() {
            public void handleResponse(HttpRequest hreq, String response) {
                JSONArray jsarr;
                try {
                    jsarr = new JSONArray(response);
                } catch (JSONException e) {
                    handleError(hreq,
                            PubnubError.PNERROBJ_5028_INVALID_JSON);
                    return;
                }
                callback.successCallback(channel, jsarr);
            }

            public void handleError(HttpRequest hreq, PubnubError error) {
                callback.errorCallback(channel, error);
                return;
            }
        });

        _request(hreq, nonSubscribeManager);
    }

    /**
     *
     * Listen for presence of subscribers on a channel
     *
     * @param channel
     *            Name of the channel on which to listen for join/leave i.e.
     *            presence events
     * @param callback
     *            object of sub class of Callback class
     * @exception PubnubException
     *                Throws PubnubException if Callback is null
     */
    public void presence(String channel, Callback callback)
            throws PubnubException {
        Hashtable args = new Hashtable(2);
        args.put("channel", channel + PRESENCE_SUFFIX);
        args.put("callback", callback);
        subscribe(args);
    }

    /**
     * Read presence information from a channel
     *
     * @param channel
     *            Channel name
     * @param callback
     *            object of sub class of Callback class
     */
    public void hereNow(final String channel, final Callback callback) {

        String[] urlargs = { getOrigin(), "v2", "presence", "sub_key",
                this.SUBSCRIBE_KEY, "channel", channel };

        HttpRequest hreq = new HttpRequest(urlargs, params,
                new ResponseHandler() {
            public void handleResponse(HttpRequest hreq, String response) {
                JSONObject jsobj;
                try {
                    jsobj = new JSONObject(response);
                } catch (JSONException e) {
                    callback.errorCallback(channel,
                            PubnubError.PNERROBJ_5004_JSON_ERROR);
                    return;
                }
                callback.successCallback(channel, jsobj);
            }

            public void handleError(HttpRequest hreq, PubnubError error) {
                callback.errorCallback(channel, error);
                return;
            }
        });

        _request(hreq, nonSubscribeManager);
    }

    /**
     *
     * Read history from a channel.
     *
     * @param channel
     *            Channel Name
     * @param limit
     *            Upper limit on number of messages in response
     *
     */
    public void history(String channel, int limit, Callback callback) {
        Hashtable args = new Hashtable(2);
        args.put("channel", channel);
        args.put("limit", String.valueOf(limit));
        args.put("callback", callback);
        history(args);
    }

    /**
     *
     * Read history from a channel.
     *
     * @param args
     *            Hashtable containing channel name, limit, Callback
     */
    public void history(Hashtable args) {

        final String channel = (String) args.get("channel");
        String limit = (String) args.get("limit");
        final Callback callback = (Callback) args.get("callback");

        String[] urlargs = { getOrigin(), "history", this.SUBSCRIBE_KEY,
                PubnubUtil.urlEncode(channel), "0", limit };

        HttpRequest hreq = new HttpRequest(urlargs, params,
                new ResponseHandler() {

            public void handleResponse(HttpRequest hreq, String response) {
                JSONArray respArr;
                    try {
                        respArr = new JSONArray(response);
                        decryptJSONArray(respArr);
                        callback.successCallback(channel, respArr);
                    } catch (JSONException e) {
                        callback.errorCallback(channel,
                            PubnubError.PNERROBJ_5004_JSON_ERROR);
                    }catch (DataLengthException e) {

                    } catch (IllegalStateException e) {

                    } catch (InvalidCipherTextException e) {

                    }catch (IOException e) {

                    }

            }

            public void handleError(HttpRequest hreq, PubnubError error) {
                callback.errorCallback(channel, error);
            }

        });
        _request(hreq, nonSubscribeManager);
    }

    /**
     *
     * Read DetailedHistory for a channel.
     *
     * @param channel
     *            Channel name for which detailed history is required
     * @param start
     *            Start time
     * @param end
     *            End time
     * @param count
     *            Upper limit on number of messages to be returned
     * @param reverse
     *            True if messages need to be in reverse order
     * @param callback
     *            Callback
     */
    public void detailedHistory(final String channel, long start, long end,
            int count, boolean reverse, final Callback callback) {
        Hashtable parameters = hashtableClone(params);
        if (count == -1)
            count = 100;

        parameters.put("count", String.valueOf(count));
        parameters.put("reverse", String.valueOf(reverse));

        if (start != -1)
            parameters.put("start", Long.toString(start).toLowerCase());

        if (end != -1)
            parameters.put("end", Long.toString(end).toLowerCase());

        String[] urlargs = { getOrigin(), "v2", "history", "sub-key",
                this.SUBSCRIBE_KEY, "channel", PubnubUtil.urlEncode(channel) };

        HttpRequest hreq = new HttpRequest(urlargs, parameters,
                new ResponseHandler() {

            public void handleResponse(HttpRequest hreq, String response) {
                JSONArray respArr;
                try {
                    respArr = new JSONArray(response);
                    decryptJSONArray((JSONArray) respArr.get(0));
                    callback.successCallback(channel, respArr);
                } catch (JSONException e) {
                    callback.errorCallback(channel,
                            PubnubError.PNERROBJ_5005_JSON_ERROR);
                } catch (DataLengthException e) {
                    callback.errorCallback(channel,
                            PubnubError.PNERROBJ_5040_DECRYPTION_ERROR);
                } catch (IllegalStateException e) {
                    callback.errorCallback(channel,
                            PubnubError.PNERROBJ_5041_DECRYPTION_ERROR);
                } catch (InvalidCipherTextException e) {
                    callback.errorCallback(channel,
                            PubnubError.PNERROBJ_5042_DECRYPTION_ERROR);
                } catch (IOException e) {
                    callback.errorCallback(channel,
                            PubnubError.PNERROBJ_5043_DECRYPTION_ERROR);
                }

            }

            public void handleError(HttpRequest hreq, PubnubError error) {
                callback.errorCallback(channel, error);
                return;
            }

        });
        _request(hreq, nonSubscribeManager);
    }

    /**
     *
     * Read DetailedHistory for a channel.
     *
     * @param channel
     *            Channel name for which detailed history is required
     * @param start
     *            Start time
     * @param reverse
     *            True if messages need to be in reverse order
     * @param callback
     *            Callback
     */
    public void detailedHistory(String channel, long start, boolean reverse,
            Callback callback) {
        detailedHistory(channel, start, -1, -1, reverse, callback);
    }

    /**
     *
     * Read DetailedHistory for a channel.
     *
     * @param channel
     *            Channel name for which detailed history is required
     * @param start
     *            Start time
     * @param end
     *            End time
     * @param callback
     *            Callback
     */
    public void detailedHistory(String channel, long start, long end,
            Callback callback) {
        detailedHistory(channel, start, end, -1, false, callback);
    }

    /**
     *
     * Read DetailedHistory for a channel.
     *
     * @param channel
     *            Channel name for which detailed history is required
     * @param start
     *            Start time
     * @param end
     *            End time
     * @param reverse
     *            True if messages need to be in reverse order
     *
     * @param callback
     *            Callback
     */
    public void detailedHistory(String channel, long start, long end,
            boolean reverse, Callback callback) {
        detailedHistory(channel, start, end, -1, reverse, callback);
    }

    /**
     *
     * Read DetailedHistory for a channel.
     *
     * @param channel
     *            Channel name for which detailed history is required
     * @param count
     *            Upper limit on number of messages to be returned
     * @param reverse
     *            True if messages need to be in reverse order
     * @param callback
     *            Callback
     */
    public void detailedHistory(String channel, int count, boolean reverse,
            Callback callback) {
        detailedHistory(channel, -1, -1, count, reverse, callback);
    }

    /**
     *
     * Read DetailedHistory for a channel.
     *
     * @param channel
     *            Channel name for which detailed history is required
     * @param reverse
     *            True if messages need to be in reverse order
     * @param callback
     *            Callback
     */
    public void detailedHistory(String channel, boolean reverse,
            Callback callback) {
        detailedHistory(channel, -1, -1, -1, reverse, callback);
    }

    /**
     *
     * Read DetailedHistory for a channel.
     *
     * @param channel
     *            Channel name for which detailed history is required
     * @param count
     *            Maximum number of messages
     * @param callback
     *            Callback object
     */
    public void detailedHistory(String channel, int count, Callback callback) {
        detailedHistory(channel, -1, -1, count, false, callback);
    }

    /**
     * Read current time from PubNub Cloud.
     *
     * @param callback
     *            Callback object
     */
    public void time(final Callback callback) {

        String[] url = { getOrigin(), "time", "0" };
        HttpRequest hreq = new HttpRequest(url, params, new ResponseHandler() {

            public void handleResponse(HttpRequest hreq, String response) {
                callback.successCallback(null, response);
            }

            public void handleError(HttpRequest hreq, PubnubError error) {
                callback.errorCallback(null, error);
            }

        });

        _request(hreq, nonSubscribeManager);
    }

    private boolean inputsValid(Hashtable args) throws PubnubException {
        boolean channelMissing;
        if (((Callback) args.get("callback")) == null) {
            throw new PubnubException("Invalid Callback");
        }
        Object _channels = args.get("channels");
        Object _channel = args.get("channel");

        channelMissing = ((_channel == null || _channel.equals("")) && (_channels == null || _channels
                .equals(""))) ? true : false;

        if (channelMissing) {
            throw new PubnubException("Channel Missing");
        }
        return true;
    }

    private void leave(final String channel) {

        String[] urlargs = { getOrigin(), "v2/presence/sub_key",
                this.SUBSCRIBE_KEY, "channel", PubnubUtil.urlEncode(channel),
        "leave" };
        Hashtable params = new Hashtable();
        params.put("uuid", UUID);

        HttpRequest hreq = new HttpRequest(urlargs, params,
                new ResponseHandler() {

            public void handleResponse(HttpRequest hreq, String response) {

            }

            public void handleError(HttpRequest hreq, PubnubError error) {

            }

        });
        _request(hreq, nonSubscribeManager);
    }

    /**
     * Unsubscribe from channels.
     *
     * @param channels
     *            String array containing channel names
     */
    public void unsubscribe(String[] channels) {
        for (int i = 0; i < channels.length; i++) {
            subscriptions.removeChannel(channels[i]);
            leave(channels[i]);
        }
        resubscribe();
    }

    /**
     * Unsubscribe from all channel.
     *
     */
    public void unsubscribeAll() {
        subscriptions.removeAllChannels();
        disconnectAndResubscribe();
    }

    /**
     * Unsubscribe from presence channel.
     *
     * @param channel
     *            channel name as String.
     */
    public void unsubscribePresence(String channel) {
        unsubscribe(new String[] { channel + PRESENCE_SUFFIX });
    }

    /**
     * Unsubscribe/Disconnect from channel.
     *
     * @param channel
     *            channel name as String.
     */
    public void unsubscribe(String channel) {
        unsubscribe(new String[] { channel });
    }

    /**
     * Unsubscribe/Disconnect from channel.
     *
     * @param args
     *            Hashtable containing channel name.
     */
    public void unsubscribe(Hashtable args) {
        String[] channelList = (String[]) args.get("channels");
        if (channelList == null) {
            channelList = new String[] { (String) args.get("channel") };
        }
        unsubscribe(channelList);
    }

    /**
     *
     * Listen for a message on a channel.
     *
     * @param args
     *            Hashtable containing channel name
     * @param callback
     *            Callback
     * @exception PubnubException
     *                Throws PubnubException if Callback is null
     */
    public void subscribe(Hashtable args, Callback callback)
            throws PubnubException {
        args.put("callback", callback);
        subscribe(args);
    }

    /**
     *
     * Listen for a message on a channel.
     *
     * @param args
     *            Hashtable containing channel name, callback
     * @exception PubnubException
     *                Throws PubnubException if Callback is null
     */
    public void subscribe(Hashtable args) throws PubnubException {

        if (!inputsValid(args)) {
            return;
        }
        _subscribe(args);
    }

    /**
     *
     * Listen for a message on a channel.
     *
     * @param channelsArr
     *            Array of channel names (string) to listen on
     * @param callback
     *            Callback
     * @exception PubnubException
     *                Throws PubnubException if Callback is null
     */
    public void subscribe(String[] channelsArr, Callback callback)
            throws PubnubException {
        subscribe(channelsArr, callback, "0");
    }

    /**
     *
     * Listen for a message on a channel.
     *
     * @param channelsArr
     *            Array of channel names (string) to listen on
     * @param callback
     *            Callback
     * @param timetoken
     *        Timetoken to use for subscribing
     * @exception PubnubException
     *                Throws PubnubException if Callback is null
     */
    public void subscribe(String[] channelsArr, Callback callback,
            String timetoken) throws PubnubException {

        Hashtable args = new Hashtable();

        args.put("channels", channelsArr);
        args.put("callback", callback);
        args.put("timetoken", timetoken);
        subscribe(args);
    }

    /**
     *
     * Listen for a message on a channel.
     *
     * @param channelsArr
     *            Array of channel names (string) to listen on
     * @param callback
     *            Callback
     * @param timetoken
     *        Timetoken to use for subscribing
     * @exception PubnubException
     *                Throws PubnubException if Callback is null
     */
    public void subscribe(String[] channelsArr, Callback callback,
            long timetoken) throws PubnubException {

        Hashtable args = new Hashtable();

        args.put("channels", channelsArr);
        args.put("callback", callback);
        args.put("timetoken", String.valueOf(timetoken));
        subscribe(args);
    }

    /**
     *
     * Listen for a message on a channel.
     *
     * @param channel
     *            Name of the channel
     * @param callback
     *            Callback
     * @exception PubnubException
     *                Throws PubnubException if Callback is null
     */
    public void subscribe(String channel, Callback callback)
            throws PubnubException {
        subscribe(channel, callback, "0");
    }

    /**
     *
     * Listen for a message on a channel.
     *
     * @param channel
     *            Name of the channel
     * @param callback
     *            Callback
     * @exception PubnubException
     *                Throws PubnubException if Callback is null
     */
    public void subscribe(String channel, Callback callback,
            String timetoken) throws PubnubException {

        Hashtable args = new Hashtable();

        args.put("channel", channel);
        args.put("callback", callback);
        args.put("timetoken", timetoken);
        subscribe(args);
    }

    /**
     *
     * Listen for a message on a channel.
     *
     * @param channel
     *            Name of the channel
     * @param callback
     *            Callback
     * @param timetoken
     *        Timetoken to use for subscribing
     * @exception PubnubException
     *                Throws PubnubException if Callback is null
     */
    public void subscribe(String channel, Callback callback,
            long timetoken) throws PubnubException {

        Hashtable args = new Hashtable();

        args.put("channel", channel);
        args.put("callback", callback);
        args.put("timetoken", String.valueOf(timetoken));
        subscribe(args);
    }

    private void callErrorCallbacks(String[] channelList, PubnubError error) {
        for (int i = 0; i < channelList.length; i++) {
            Callback cb = ((Channel) subscriptions.getChannel(channelList[i])).callback;
            cb.errorCallback(channelList[i], error);
        }
    }

    private void decryptJSONArray(JSONArray messages) throws JSONException, DataLengthException, IllegalStateException, InvalidCipherTextException, IOException {

        if (CIPHER_KEY.length() > 0) {
            for (int i = 0; i < messages.length(); i++) {
                PubnubCrypto pc = new PubnubCrypto(CIPHER_KEY);

                String message;
                message = pc.decrypt(messages.get(i).toString());
                messages.put(i, stringToJSON(message));            }
        }
    }

    /**
     * @param args
     *            Hashtable
     */
    private void _subscribe(Hashtable args) {

        String[] channelList = (String[]) args.get("channels");
        if (channelList == null) {
            channelList = new String[] { (String) args.get("channel") };
        }
        Callback callback = (Callback) args.get("callback");
        String timetoken = (String) args.get("timetoken");

        if (!_timetoken.equals("0"))
            _saved_timetoken = _timetoken;
        _timetoken = (timetoken == null) ? "0" : timetoken;

        /*
         * Scan through the channels array. If a channel does not exist in
         * hashtable create a new entry with default values. If already exists
         * and connected, then return
         */

        for (int i = 0; i < channelList.length; i++) {
            String channel = channelList[i];
            Channel channelObj = (Channel) subscriptions.getChannel(channel);

            if (channelObj == null) {
                Channel ch = new Channel();
                ch.name = channel;
                ch.connected = false;
                ch.callback = callback;
                subscriptions.addChannel(ch);
            } else if (channelObj.connected) {

                return;
            }
        }
        _subscribe_base(true);
    }

    private void _subscribe_base(boolean fresh) {
        _subscribe_base(fresh, false, null);
    }
    private void _subscribe_base(boolean fresh, boolean dar) {
        _subscribe_base(fresh, dar, null);
    }

    private void _subscribe_base(Worker worker) {
        _subscribe_base(false, false, worker);
    }

    private void _subscribe_base(boolean fresh, Worker worker) {
        _subscribe_base(fresh, false, worker);
    }

    private boolean isWorkerDead(HttpRequest hreq) {
        return (hreq == null || hreq.getWorker() == null)?false:hreq.getWorker()._die;
    }
    private void _subscribe_base(boolean fresh, boolean dar, Worker worker) {
        String channelString = subscriptions.getChannelString();
        String[] channelsArray = subscriptions.getChannelNames();
        if (channelsArray.length <= 0)
            return;

        if (channelString == null) {
            callErrorCallbacks(channelsArray,
                    PubnubError.PNERROBJ_5026_PARSING_ERROR);
            return;
        }
        String[] urlComponents = { getOrigin(), "subscribe",
                PubnubCore.this.SUBSCRIBE_KEY,
                PubnubUtil.urlEncode(channelString), "0", _timetoken };


        Hashtable params = hashtableClone(this.params);
        params.put("uuid", UUID);
        log.verbose("Subscribing with timetoken : " + _timetoken);

        HttpRequest hreq = new HttpRequest(urlComponents, params,
                new ResponseHandler() {

            public void handleResponse(HttpRequest hreq, String response) {

                /*
                 * Check if response has channel names. A JSON response
                 * with more than 2 items means the response contains
                 * the channel names as well. The channel names are in a
                 * comma delimted string. Call success callback on all
                 * he channels passing the corresponding response
                 * message.
                 */

                JSONArray jsa;
                try {
                    jsa = new JSONArray(response);

                    _timetoken = (!_saved_timetoken.equals("0") && isResumeOnReconnect()) ? _saved_timetoken
                            : jsa.get(1).toString();
                    log.verbose("Resume On Reconnect is "
                            + isResumeOnReconnect());
                    log.verbose("Saved Timetoken : " + _saved_timetoken);
                    log.verbose("In Response Timetoken : "
                            + jsa.get(1).toString());
                    log.verbose("Timetoken value set to " + _timetoken);
                    _saved_timetoken = "0";
                    log.verbose("Saved Timetoken reset to 0");
                    if (!hreq.isDar()) {
                        subscriptions
                        .invokeConnectCallbackOnChannels(_timetoken);
                    } else {
                        subscriptions
                        .invokeReconnectCallbackOnChannels(_timetoken);
                    }
                    JSONArray messages = new JSONArray(jsa.get(0)
                            .toString());

                    if (jsa.length() > 2) {
                        /*
                         * Response has multiple channels
                         */

                        String[] _channels = PubnubUtil.splitString(
                                jsa.getString(2), ",");

                        for (int i = 0; i < _channels.length; i++) {
                            Channel _channel = (Channel) subscriptions
                                    .getChannel(_channels[i]);
                            if (_channel != null) {
                                JSONObject jsobj = null;
                                if (CIPHER_KEY.length() > 0
                                        && !_channel.name
                                        .endsWith(PRESENCE_SUFFIX)) {
                                    PubnubCrypto pc = new PubnubCrypto(
                                            CIPHER_KEY);
                                    try {
                                        String message = pc
                                                .decrypt(messages
                                                        .get(i)
                                                        .toString());
                                        if(!isWorkerDead(hreq))  _channel.callback
                                        .successCallback(
                                                _channel.name,
                                                stringToJSON(message));
                                    } catch (DataLengthException e) {
                                        if(!isWorkerDead(hreq)) _channel.callback
                                        .errorCallback(
                                                _channel.name,
                                                PubnubError.PNERROBJ_5044_DECRYPTION_ERROR);
                                    } catch (IllegalStateException e) {
                                        if(!isWorkerDead(hreq)) _channel.callback
                                        .errorCallback(
                                                _channel.name,
                                                PubnubError.PNERROBJ_5045_DECRYPTION_ERROR);
                                    } catch (InvalidCipherTextException e) {
                                        if(!isWorkerDead(hreq)) _channel.callback
                                        .errorCallback(
                                                _channel.name,
                                                PubnubError.PNERROBJ_5046_DECRYPTION_ERROR);
                                    } catch (IOException e) {
                                        if(!isWorkerDead(hreq)) _channel.callback
                                        .errorCallback(
                                                _channel.name,
                                                PubnubError.PNERROBJ_5047_DECRYPTION_ERROR);
                                    }

                                } else {
                                    if(!isWorkerDead(hreq)) _channel.callback.successCallback(
                                            _channel.name,
                                            messages.get(i));
                                }
                            }
                        }

                    } else {
                        /*
                         * Response for single channel Callback on
                         * single channel
                         */
                        Channel _channel = subscriptions
                                .getFirstChannel();

                        if (_channel != null) {
                            for (int i = 0; i < messages.length(); i++) {
                                if (CIPHER_KEY.length() > 0
                                        && !_channel.name
                                        .endsWith(PRESENCE_SUFFIX)) {
                                    PubnubCrypto pc = new PubnubCrypto(
                                            CIPHER_KEY);
                                    try {
                                        String message = pc
                                                .decrypt(messages
                                                        .get(i)
                                                        .toString());
                                        if(!isWorkerDead(hreq)) _channel.callback
                                        .successCallback(
                                                _channel.name,
                                                stringToJSON(message));
                                    } catch (DataLengthException e) {
                                        if(!isWorkerDead(hreq)) _channel.callback
                                        .errorCallback(
                                                _channel.name,
                                                PubnubError.PNERROBJ_5048_DECRYPTION_ERROR);
                                    } catch (IllegalStateException e) {
                                        if(!isWorkerDead(hreq)) _channel.callback
                                        .errorCallback(
                                                _channel.name,
                                                PubnubError.PNERROBJ_5049_DECRYPTION_ERROR);
                                    } catch (InvalidCipherTextException e) {
                                        if(!isWorkerDead(hreq)) _channel.callback
                                        .errorCallback(
                                                _channel.name,
                                                PubnubError.PNERROBJ_5050_DECRYPTION_ERROR);
                                    } catch (IOException e) {
                                        if(!isWorkerDead(hreq)) _channel.callback
                                        .errorCallback(
                                                _channel.name,
                                                PubnubError.PNERROBJ_5051_DECRYPTION_ERROR);
                                    }
                                } else {
                                    if(!isWorkerDead(hreq)) _channel.callback.successCallback(
                                            _channel.name,
                                            messages.get(i));
                                }

                            }
                        }

                    }
                    if (hreq.isSubzero()) {
                        log.verbose("Response of subscribe 0 request. Need to do dAr process again");
                        _subscribe_base(false, hreq.isDar(), hreq.getWorker());
                    } else
                        _subscribe_base(false);
                } catch (JSONException e) {
                    if (hreq.isSubzero()) {
                        log.verbose("Response of subscribe 0 request. Need to do dAr process again");
                        _subscribe_base(false, hreq.isDar(), hreq.getWorker());
                    } else
                        _subscribe_base(false, hreq.getWorker());
                }

            }

            public void handleBackFromDar(HttpRequest hreq) {
                _subscribe_base(false, hreq.getWorker());
            }

            public void handleError(HttpRequest hreq, PubnubError error) {
                disconnectAndResubscribe();
            }

            public void handleTimeout(HttpRequest hreq) {
                log.verbose("Timeout Occurred, Calling disconnect callbacks on the channels");
                String timeoutTimetoken = (isResumeOnReconnect()) ? (_timetoken
                        .equals("0")) ? _saved_timetoken : _timetoken
                                : "0";
                log.verbose("Timeout Timetoken : " + timeoutTimetoken);
                subscriptions
                .invokeDisconnectCallbackOnChannels(timeoutTimetoken);
                // disconnectAndResubscribe();

                // subscriptions.removeAllChannels();
            }

            public String getTimetoken() {
                return _timetoken;
            }
        });
        if (_timetoken.equals("0")) {
            hreq.setSubzero(true);
            log.verbose("This is a subscribe 0 request");
        }
        hreq.setDar(dar);
        if ( worker != null && worker instanceof Worker )
            hreq.setWorker(worker);
        _request(hreq, subscribeManager, fresh);
    }

    /**
     * @param req
     * @param connManager
     * @param abortExisting
     */
    private void _request(final HttpRequest hreq, RequestManager connManager,
            boolean abortExisting) {
        if (abortExisting) {
            connManager.resetHttpManager();
        }
        connManager.queue(hreq);
    }

    /**
     * @param req
     * @param simpleConnManager2
     */
    private void _request(final HttpRequest hreq,
            RequestManager simpleConnManager) {
        _request(hreq, simpleConnManager, false);
    }

    private int getRandom() {
        return this.generator.nextInt();
    }

    private void changeOrigin() {
        this.ORIGIN_STR = null;
        this.HOSTNAME_SUFFIX = getRandom();
    }

    private void resubscribe() {
        changeOrigin();
        if (!_timetoken.equals("0"))
            _saved_timetoken = _timetoken;
        _timetoken = "0";
        log.verbose("Before Resubscribe Timetoken : " + _timetoken);
        log.verbose("Before Resubscribe Saved Timetoken : " + _saved_timetoken);
        _subscribe_base(true, true);
    }

    private void resubscribe(String timetoken) {
        changeOrigin();
        if (!timetoken.equals("0"))
            _saved_timetoken = timetoken;
        _timetoken = "0";
        log.verbose("Before Resubscribe Timetoken : " + _timetoken);
        log.verbose("Before Resubscribe Saved Timetoken : " + _saved_timetoken);
        _subscribe_base(true, true);
    }

    /**
     * Disconnect from all channels, and resubscribe
     *
     */
    public void disconnectAndResubscribeWithTimetoken(String timetoken) {
        disconnectAndResubscribeWithTimetoken(timetoken,
                PubnubError.PNERROBJ_5030_DISCONN_AND_RESUB);
    }

    /**
     * Disconnect from all channels, and resubscribe
     *
     */
    public void disconnectAndResubscribeWithTimetoken(String timetoken,
            PubnubError error) {
        log.verbose("Received disconnectAndResubscribeWithTimetoken");
        subscriptions.invokeErrorCallbackOnChannels(error);
        resubscribe(timetoken);
    }

    /**
     * Disconnect from all channels, and resubscribe
     *
     */
    public void disconnectAndResubscribe() {
        disconnectAndResubscribe(PubnubError.PNERROBJ_5029_DISCONNECT);
    }

    /**
     * Disconnect from all channels, and resubscribe
     *
     */
    public void disconnectAndResubscribe(PubnubError error) {
        log.verbose("Received disconnectAndResubscribe");
        subscriptions.invokeErrorCallbackOnChannels(error);
        resubscribe();
    }

    /**
     * This method returns array of channel names, currently subscribed to
     *
     * @return Array of channel names
     */
    public String[] getSubscribedChannelsArray() {
        return subscriptions.getChannelNames();
    }

    /**
     * Sets origin value, default is "pubsub"
     *
     * @param origin
     *            Origin value
     */
    public void setOrigin(String origin) {
        this.HOSTNAME = origin;
    }

    /**
     * This method return auth key. Return null if not set
     *
     * @return Auth Key. null if auth key not set
     */
    public String getAuthKey() {
        return this.AUTH_STR;
    }

    /**
     * This method sets auth key.
     *
     * @param authKey
     *            . 0 length string or null unsets auth key
     */
    public void setAuthKey(String authKey) {

        this.AUTH_STR = authKey;
        if (authKey == null || authKey.length() == 0) {
            params.remove("auth");
        } else {
            params.put("auth", this.AUTH_STR);
        }
        resubscribe();
    }

    /**
     * This method unsets auth key.
     *
     */
    public void unsetAuthKey() {
        this.AUTH_STR = null;
        params.remove("auth");
        resubscribe();
    }
}
