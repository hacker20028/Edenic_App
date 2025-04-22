package com.aryan.edenic.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.aryan.edenic.R;
import com.aryan.edenic.models.ChatMessage;
import com.aryan.edenic.models.Stock;
import com.aryan.edenic.utils.StockLogoLoader;
import com.aryan.edenic.yahoo_finance.YahooFinanceClient;
import com.aryan.edenic.yahoo_finance.YahooResponse;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_SENT_TEXT = 0;
    private static final int VIEW_TYPE_RECEIVED_TEXT = 1;
    private static final int VIEW_TYPE_SENT_STOCK = 2;
    private static final int VIEW_TYPE_RECEIVED_STOCK = 3;

    private final Context context;
    private final List<ChatMessage> messages;
    private final String currentUserId;

    public ChatMessageAdapter(Context context, List<ChatMessage> messages, String currentUserId) {
        this.context = context;
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        boolean isSender = message.getSenderId().equals(currentUserId);

        if (message.getType() == ChatMessage.MessageType.TEXT) {
            return isSender ? VIEW_TYPE_SENT_TEXT : VIEW_TYPE_RECEIVED_TEXT;
        } else {
            return isSender ? VIEW_TYPE_SENT_STOCK : VIEW_TYPE_RECEIVED_STOCK;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT_TEXT) {
            View view = LayoutInflater.from(context).inflate(
                    R.layout.item_message_sent, parent, false);
            return new SentTextViewHolder(view);
        } else if (viewType == VIEW_TYPE_RECEIVED_TEXT) {
            View view = LayoutInflater.from(context).inflate(
                    R.layout.item_message_received, parent, false);
            return new ReceivedTextViewHolder(view);
        } else if (viewType == VIEW_TYPE_SENT_STOCK) {
            View view = LayoutInflater.from(context).inflate(
                    R.layout.item_stock_message_sent, parent, false);
            return new SentStockViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(
                    R.layout.item_stock_message_received, parent, false);
            return new ReceivedStockViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);

        if (holder instanceof SentTextViewHolder) {
            bindSentTextMessage((SentTextViewHolder) holder, message);
        } else if (holder instanceof ReceivedTextViewHolder) {
            bindReceivedTextMessage((ReceivedTextViewHolder) holder, message);
        } else if (holder instanceof SentStockViewHolder) {
            bindSentStockMessage((SentStockViewHolder) holder, message);
        } else if (holder instanceof ReceivedStockViewHolder) {
            bindReceivedStockMessage((ReceivedStockViewHolder) holder, message);
        }
    }

    private void bindSentTextMessage(SentTextViewHolder holder, ChatMessage message) {
        holder.messageContent.setText(message.getContent());
        holder.messageTime.setText(formatTime(message.getTimestamp()));

        // Show read status
        holder.readStatus.setVisibility(message.isRead() ? View.VISIBLE : View.GONE);
    }

    private void bindReceivedTextMessage(ReceivedTextViewHolder holder, ChatMessage message) {
        holder.messageContent.setText(message.getContent());
        holder.messageTime.setText(formatTime(message.getTimestamp()));
    }

    private void bindSentStockMessage(SentStockViewHolder holder, ChatMessage message) {
        // Set stock symbol and initial price
        holder.stockSymbol.setText(message.getStockSymbol());
        holder.stockPrice.setText(String.format(Locale.US, "$%.2f", message.getStockPrice()));
        holder.messageTime.setText(formatTime(message.getTimestamp()));

        // Load stock logo
        StockLogoLoader.loadStockLogo(context, message.getStockSymbol(), holder.stockLogo);

        // Show read status
        holder.readStatus.setVisibility(message.isRead() ? View.VISIBLE : View.GONE);

        // Fetch latest price
        updateStockPrice(message, holder.stockPrice, holder.priceChange);

        // Set click listener to open stock details
        holder.stockCard.setOnClickListener(v -> openStockDetails(message.getStockSymbol()));
    }

    private void bindReceivedStockMessage(ReceivedStockViewHolder holder, ChatMessage message) {
        // Set stock symbol and initial price
        holder.stockSymbol.setText(message.getStockSymbol());
        holder.stockPrice.setText(String.format(Locale.US, "$%.2f", message.getStockPrice()));
        holder.messageTime.setText(formatTime(message.getTimestamp()));

        // Load stock logo
        StockLogoLoader.loadStockLogo(context, message.getStockSymbol(), holder.stockLogo);

        // Fetch latest price
        updateStockPrice(message, holder.stockPrice, holder.priceChange);

        // Set click listener to open stock details
        holder.stockCard.setOnClickListener(v -> openStockDetails(message.getStockSymbol()));
    }

    private void updateStockPrice(ChatMessage message, TextView priceView, TextView changeView) {
        YahooFinanceClient.getInstance().getStockData(message.getStockSymbol(), "1d", "1d")
                .enqueue(new Callback<YahooResponse>() {
                    @Override
                    public void onResponse(Call<YahooResponse> call, Response<YahooResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            YahooResponse yahooResponse = response.body();

                            if (yahooResponse.chart != null &&
                                    yahooResponse.chart.result != null &&
                                    !yahooResponse.chart.result.isEmpty() &&
                                    yahooResponse.chart.result.get(0) != null &&
                                    yahooResponse.chart.result.get(0).meta != null) {

                                double currentPrice = yahooResponse.chart.result.get(0).meta.regularMarketPrice;
                                double prevClose = yahooResponse.chart.result.get(0).meta.previousClose;

                                // Update price in UI
                                priceView.setText(String.format(Locale.US, "$%.2f", currentPrice));

                                // Calculate and display change percentage
                                if (prevClose > 0) {
                                    double change = currentPrice - prevClose;
                                    double changePercent = (change / prevClose) * 100;

                                    String changeText = String.format(Locale.US, "%s%.2f%%",
                                            changePercent >= 0 ? "+" : "", changePercent);

                                    changeView.setText(changeText);

                                    // Set color based on change
                                    int color = ContextCompat.getColor(context,
                                            changePercent >= 0 ? R.color.green : R.color.red);
                                    changeView.setTextColor(color);
                                    changeView.setVisibility(View.VISIBLE);
                                } else {
                                    changeView.setVisibility(View.GONE);
                                }

                                // Update price in Firebase to keep it current
                                updateMessagePrice(message.getMessageId(), currentPrice);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<YahooResponse> call, Throwable t) {
                        // Stock update failed, but message still shows
                        changeView.setVisibility(View.GONE);
                    }
                });
    }

    private void updateMessagePrice(String messageId, double newPrice) {
        // Find the chat ID from the first message
        if (!messages.isEmpty()) {
            String senderId = messages.get(0).getSenderId();
            String receiverId = senderId.equals(currentUserId) ?
                    messages.get(0).getSenderId() : currentUserId;

            // Generate chat ID (alphabetical order of user IDs)
            String chatId = senderId.compareTo(receiverId) < 0 ?
                    senderId + "_" + receiverId : receiverId + "_" + senderId;

            // Update price in Firebase
            FirebaseDatabase.getInstance().getReference("messages")
                    .child(chatId)
                    .child(messageId)
                    .child("stockPrice")
                    .setValue(newPrice);
        }
    }

    private void openStockDetails(String symbol) {
        // Create a stock object and show trade dialog
        Stock stock = new Stock(symbol, getCompanyName(symbol), 0, 0);

        // Fetch current price
        YahooFinanceClient.getInstance().getStockData(symbol, "1d", "1d")
                .enqueue(new Callback<YahooResponse>() {
                    @Override
                    public void onResponse(Call<YahooResponse> call, Response<YahooResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            YahooResponse yahooResponse = response.body();

                            if (yahooResponse.chart != null &&
                                    yahooResponse.chart.result != null &&
                                    !yahooResponse.chart.result.isEmpty() &&
                                    yahooResponse.chart.result.get(0) != null &&
                                    yahooResponse.chart.result.get(0).meta != null) {

                                double currentPrice = yahooResponse.chart.result.get(0).meta.regularMarketPrice;
                                stock.setPrice(currentPrice);

                                // Show trade dialog
                                showTradeDialog(stock);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<YahooResponse> call, Throwable t) {
                        // Handle error
                    }
                });
    }

    private void showTradeDialog(Stock stock) {
        // Implementation will depend on your existing trade dialog code
        // You might want to create a method in HomeActivity and call it from here
    }

    private String getCompanyName(String symbol) {
        // Map symbols to company names (simplified)
        switch (symbol) {
            case "AAPL": return "Apple Inc.";
            case "MSFT": return "Microsoft Corporation";
            case "GOOGL": return "Alphabet Inc.";
            case "AMZN": return "Amazon.com Inc.";
            case "META": return "Meta Platforms Inc.";
            case "TSLA": return "Tesla Inc.";
            default: return symbol;
        }
    }

    private String formatTime(long timestamp) {
        Date date = new Date(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        return sdf.format(date);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    // ViewHolders
    static class SentTextViewHolder extends RecyclerView.ViewHolder {
        TextView messageContent;
        TextView messageTime;
        TextView readStatus;

        SentTextViewHolder(View itemView) {
            super(itemView);
            messageContent = itemView.findViewById(R.id.message_content);
            messageTime = itemView.findViewById(R.id.message_time);
            readStatus = itemView.findViewById(R.id.read_status);
        }
    }

    static class ReceivedTextViewHolder extends RecyclerView.ViewHolder {
        TextView messageContent;
        TextView messageTime;

        ReceivedTextViewHolder(View itemView) {
            super(itemView);
            messageContent = itemView.findViewById(R.id.message_content);
            messageTime = itemView.findViewById(R.id.message_time);
        }
    }

    static class SentStockViewHolder extends RecyclerView.ViewHolder {
        CardView stockCard;
        ImageView stockLogo;
        TextView stockSymbol;
        TextView stockPrice;
        TextView priceChange;
        TextView messageTime;
        TextView readStatus;

        SentStockViewHolder(View itemView) {
            super(itemView);
            stockCard = itemView.findViewById(R.id.stock_card);
            stockLogo = itemView.findViewById(R.id.stock_logo);
            stockSymbol = itemView.findViewById(R.id.stock_symbol);
            stockPrice = itemView.findViewById(R.id.stock_price);
            priceChange = itemView.findViewById(R.id.price_change);
            messageTime = itemView.findViewById(R.id.message_time);
            readStatus = itemView.findViewById(R.id.read_status);
        }
    }

    static class ReceivedStockViewHolder extends RecyclerView.ViewHolder {
        CardView stockCard;
        ImageView stockLogo;
        TextView stockSymbol;
        TextView stockPrice;
        TextView priceChange;
        TextView messageTime;

        ReceivedStockViewHolder(View itemView) {
            super(itemView);
            stockCard = itemView.findViewById(R.id.stock_card);
            stockLogo = itemView.findViewById(R.id.stock_logo);
            stockSymbol = itemView.findViewById(R.id.stock_symbol);
            stockPrice = itemView.findViewById(R.id.stock_price);
            priceChange = itemView.findViewById(R.id.price_change);
            messageTime = itemView.findViewById(R.id.message_time);
        }
    }
}