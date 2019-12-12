package com.coders.facedetection;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.MessageFormat;
import java.util.List;

public class FaceDetectionAdapter extends RecyclerView.Adapter<FaceDetectionAdapter.ViewHolder> {
    private List<FaceDetectionModel> faceDetectionModelList;
    private Context context;

    public FaceDetectionAdapter(List<FaceDetectionModel> faceDetectionModelList, Context context) {
        this.faceDetectionModelList = faceDetectionModelList;
        this.context = context;
    }

    @NonNull
    @Override
    public FaceDetectionAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_face_detection, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FaceDetectionAdapter.ViewHolder viewHolder, int i) {

        FaceDetectionModel faceDetectionModel = faceDetectionModelList.get(i);
        viewHolder.text1.setText(MessageFormat.format("Face: {0}", String.valueOf(faceDetectionModel.getId())));
        viewHolder.text2.setText(MessageFormat.format("Face: {0}", faceDetectionModel.getText()));
    }

    @Override
    public int getItemCount() {
        return faceDetectionModelList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView text1;
        public TextView text2;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            text1 = itemView.findViewById(R.id.item_face_detection_text_view1);
            text2 = itemView.findViewById(R.id.item_face_detection_text_view2);
        }
    }
}
