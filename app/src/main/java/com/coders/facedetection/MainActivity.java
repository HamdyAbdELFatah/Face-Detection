package com.coders.facedetection;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;
import com.theartofdev.edmodo.cropper.CropImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.lang.Float.max;
import static java.lang.Math.abs;
import static java.lang.Math.atan2;

public class MainActivity extends AppCompatActivity  {

    private Facing cameraFacing = Facing.FRONT;
    private ImageView imageView;
    private FrameLayout frameLayout;
    private FloatingActionButton glass;
    private FloatingActionButton landmark;
    private CameraView faceDetectionCameraView;
    private RecyclerView bottomSheetRecyclerView;
    private BottomSheetBehavior bottomSheetBehavior;
    private ArrayList<FaceDetectionModel> faceDetectionModels;
    List<FirebaseVisionFace> current_firebaseVisionFaces;
    FloatingActionButton toggleButton;
    Bitmap current_bitmap;
    FrameProcessor frameProcessor;
    private int flag=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        frameLayout=findViewById(R.id.face_detection_camera_container);
        faceDetectionModels = new ArrayList<>();
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet));

        imageView = findViewById(R.id.face_detection_image_view);
        glass = findViewById(R.id.image_glass);
        landmark = findViewById(R.id.image_landmark);
        faceDetectionCameraView = findViewById(R.id.face_detection_camera_view);
        toggleButton = findViewById(R.id.face_detection_camera_toggle_button);
        FrameLayout bottomSheetButton = findViewById(R.id.bottom_sheet_button);
        bottomSheetRecyclerView = findViewById(R.id.bottom_sheet_recycler_view);

        faceDetectionCameraView.setFacing(cameraFacing);
        faceDetectionCameraView.setLifecycleOwner(MainActivity.this);
        frameProcessor=new FrameProcessor(){
            @Override
            public void process(@NonNull Frame frame) {
                try {
                    Thread.sleep(600);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                processed(frame);
            }
        };
        faceDetectionCameraView.addFrameProcessor(frameProcessor);


        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraFacing = (cameraFacing == Facing.FRONT) ? Facing.BACK : Facing.FRONT;
                // cameraFacing = (cameraFacing == Facing.FRONT) ? Facing.BACK : Facing.FRONT;
                faceDetectionCameraView.setFacing(cameraFacing);
            }
        });

        bottomSheetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CropImage.activity().start(MainActivity.this);
            }
        });

        bottomSheetRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        bottomSheetRecyclerView.setAdapter(new FaceDetectionAdapter(faceDetectionModels, this));

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK) {
                assert result != null;
                Uri imageUri = result.getUri();
                try {
                    analyzeImage(MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @SuppressLint("RestrictedApi")
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void analyzeImage(final Bitmap bitmap) {
        if (bitmap == null) {
            Toast.makeText(MainActivity.this, "There was an error", Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        imageView.setImageBitmap(null);
        faceDetectionModels.clear();
        faceDetectionCameraView.removeFrameProcessor(frameProcessor);
        faceDetectionCameraView.removeAllViews();
        faceDetectionCameraView.setVisibility(View.GONE);
        frameLayout.setVisibility(View.GONE);
        faceDetectionCameraView.clearFrameProcessors();
        faceDetectionCameraView.close();
        faceDetectionCameraView.destroy();
        glass.setVisibility(View.INVISIBLE);
        landmark.setVisibility(View.INVISIBLE);
        imageView.setImageBitmap(null);
        flag=1;
        Objects.requireNonNull(bottomSheetRecyclerView.getAdapter()).notifyDataSetChanged();
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        //show Progress
        showProgress();
       // toggleButton.setVisibility(View.GONE);

        FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(bitmap);
        FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)
                        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                        .build();

        FirebaseVisionFaceDetector faceDetector = FirebaseVision.getInstance()
                .getVisionFaceDetector(options);
        faceDetector.detectInImage(firebaseVisionImage)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
                    @SuppressLint("RestrictedApi")
                    @Override
                    public void onSuccess(List<FirebaseVisionFace> firebaseVisionFaces) {
                        Bitmap mutableImage = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                        detectFaces(firebaseVisionFaces, mutableImage);
                        current_firebaseVisionFaces=firebaseVisionFaces;
                        glass.setVisibility(View.VISIBLE);
                        landmark.setVisibility(View.VISIBLE);
                        current_bitmap=mutableImage;
                        imageView.setImageBitmap(mutableImage);

                        hideProgress();
                        Objects.requireNonNull(bottomSheetRecyclerView.getAdapter()).notifyDataSetChanged();
                        //bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "There was some error",
                                Toast.LENGTH_SHORT).show();
                        hideProgress();
                    }
                });
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void detectFaces(List<FirebaseVisionFace> firebaseVisionFaces, Bitmap bitmap) {
        if (firebaseVisionFaces == null || bitmap == null) {
            Toast.makeText(this, "There was an error", Toast.LENGTH_SHORT).show();
            return;
        }

        Canvas canvas = new Canvas(bitmap);
        Paint facePaint = new Paint();
        facePaint.setColor(Color.GREEN);
        facePaint.setStyle(Paint.Style.STROKE);
        facePaint.setStrokeWidth(8f);

        Paint faceTextPaint = new Paint();
        faceTextPaint.setColor(Color.BLUE);
        faceTextPaint.setTextSize(30f);
        faceTextPaint.setTypeface(Typeface.SANS_SERIF);

        Paint landmarkPaint = new Paint();
        landmarkPaint.setColor(Color.RED);
        landmarkPaint.setStyle(Paint.Style.FILL);
        landmarkPaint.setStrokeWidth(11f);



        for (int i = 0; i < firebaseVisionFaces.size(); i++) {
            FirebaseVisionFace face = firebaseVisionFaces.get(i);

            Toast.makeText(this, firebaseVisionFaces.size()+"", Toast.LENGTH_SHORT).show();
            canvas.drawRect(face.getBoundingBox(), facePaint);
            canvas.drawText("Face " + i, face.getBoundingBox().left
                            , // added >> to avoid errors when dividing with "/"
                    face.getBoundingBox().top,
                    faceTextPaint);

            faceDetectionModels.add(new FaceDetectionModel(i, "Smiling Probability " + face.getSmilingProbability()));
            faceDetectionModels.add(new FaceDetectionModel(i, "Left Eye Open Probability " + face.getLeftEyeOpenProbability()));
            faceDetectionModels.add(new FaceDetectionModel(i, "Right Eye Open Probability " + face.getRightEyeOpenProbability()));

        }
    }

    private void showProgress() {
        findViewById(R.id.bottom_sheet_button_image).setVisibility(View.GONE);
        findViewById(R.id.bottom_sheet_button_progress).setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        findViewById(R.id.bottom_sheet_button_image).setVisibility(View.VISIBLE);
        findViewById(R.id.bottom_sheet_button_progress).setVisibility(View.GONE);
    }

    public void processed(@NonNull Frame frame) {
            final int width = frame.getSize().getWidth();
            final int height = frame.getSize().getHeight();


            FirebaseVisionImageMetadata metadata = new FirebaseVisionImageMetadata
                    .Builder()
                    .setWidth(width)
                    .setHeight(height)
                    .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
                    .setRotation((cameraFacing == Facing.FRONT)
                            ? FirebaseVisionImageMetadata.ROTATION_270 :
                            FirebaseVisionImageMetadata.ROTATION_90)
                    .build();

            FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage
                    .fromByteArray(frame.getData(), metadata);
            FirebaseVisionFaceDetectorOptions options = new FirebaseVisionFaceDetectorOptions.Builder()
                    .setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)
                    .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                    .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                    //.setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
                    .build();

            FirebaseVisionFaceDetector faceDetector = FirebaseVision.getInstance().getVisionFaceDetector(options);
            faceDetector.detectInImage(firebaseVisionImage)
                    .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
                        @Override
                        public void onSuccess(List<FirebaseVisionFace> firebaseVisionFaces) {
                            imageView.setImageBitmap(null);

                         Bitmap tempmap = Bitmap.createBitmap(height, width, Bitmap.Config.ARGB_8888);
                         Bitmap bitmap=tempmap.copy(Bitmap.Config.ARGB_8888, true);

                        /*Canvas canvas = new Canvas(bitmap);
                        Paint dotPaint = new Paint();
                        dotPaint.setColor(Color.RED);
                        dotPaint.setStyle(Paint.Style.FILL);
                        dotPaint.setStrokeWidth(3f);

                        Paint linePaint = new Paint();
                        linePaint.setColor(Color.GREEN);
                        linePaint.setStyle(Paint.Style.STROKE);
                        linePaint.setStrokeWidth(2f);*/

                            Canvas canvas = new Canvas(bitmap);
                            Paint dotPaint = new Paint();
                            dotPaint.setColor(Color.YELLOW);
                            dotPaint.setStyle(Paint.Style.STROKE);
                            dotPaint.setStrokeWidth(3f);
                            for (FirebaseVisionFace face : firebaseVisionFaces) {
                                Rect rect = face.getBoundingBox();

                                //Toast.makeText(MainActivity.this, rect.left+" "+rect.top+" "+rect.right+" "+rect.bottom+" ", Toast.LENGTH_SHORT).show();
                                canvas.drawRect(rect.left, rect.top + 20, rect.right, rect.bottom + 65, dotPaint);
                                if (cameraFacing == Facing.FRONT) {
                                    //Flip image!
                                    Matrix matrix = new Matrix();
                                    matrix.preScale(-1f, 1f);
                                    Bitmap flippedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                                            bitmap.getWidth(), bitmap.getHeight(),
                                            matrix, true);
                                    imageView.setImageBitmap(flippedBitmap);
                                } else
                                    imageView.setImageBitmap(bitmap);
                            }
                            canvas=new Canvas();

                       /* for (FirebaseVisionFace face : firebaseVisionFaces) {
                            List<FirebaseVisionPoint> faceContours = face.getContour(
                                    FirebaseVisionFaceContour.FACE
                            ).getPoints();
                            for (int i = 0; i < faceContours.size(); i++) {
                                FirebaseVisionPoint faceContour = null;
                                if (i != (faceContours.size() - 1)) {
                                    faceContour = faceContours.get(i);
                                    canvas.drawLine(faceContour.getX(),
                                            faceContour.getY(),
                                            faceContours.get(i + 1).getX(),
                                            faceContours.get(i + 1).getY(),
                                            linePaint

                                    );

                                }
                            }

                            List<FirebaseVisionPoint> leftEyebrowTopCountours = face.getContour(
                                    FirebaseVisionFaceContour.LEFT_EYEBROW_TOP).getPoints();
                            for (int i = 0; i < leftEyebrowTopCountours.size(); i++) {
                                FirebaseVisionPoint contour = leftEyebrowTopCountours.get(i);
                                if (i != (leftEyebrowTopCountours.size() - 1))
                                    canvas.drawLine(contour.getX(), contour.getY(), leftEyebrowTopCountours.get(i + 1).getX(),leftEyebrowTopCountours.get(i + 1).getY(), linePaint);
                                canvas.drawCircle(contour.getX(), contour.getY(), 4f, dotPaint);

                            }

                            List<FirebaseVisionPoint> rightEyebrowTopCountours = face.getContour(
                                    FirebaseVisionFaceContour. RIGHT_EYEBROW_TOP).getPoints();
                            for (int i = 0; i < rightEyebrowTopCountours.size(); i++) {
                                FirebaseVisionPoint contour = rightEyebrowTopCountours.get(i);
                                if (i != (rightEyebrowTopCountours.size() - 1))
                                    canvas.drawLine(contour.getX(), contour.getY(), rightEyebrowTopCountours.get(i + 1).getX(),rightEyebrowTopCountours.get(i + 1).getY(), linePaint);
                                canvas.drawCircle(contour.getX(), contour.getY(), 4f, dotPaint);

                            }

                            List<FirebaseVisionPoint> rightEyebrowBottomCountours = face.getContour(
                                    FirebaseVisionFaceContour. RIGHT_EYEBROW_BOTTOM).getPoints();
                            for (int i = 0; i < rightEyebrowBottomCountours.size(); i++) {
                                FirebaseVisionPoint contour = rightEyebrowBottomCountours.get(i);
                                if (i != (rightEyebrowBottomCountours.size() - 1))
                                    canvas.drawLine(contour.getX(), contour.getY(), rightEyebrowBottomCountours.get(i + 1).getX(),rightEyebrowBottomCountours.get(i + 1).getY(), linePaint);
                                canvas.drawCircle(contour.getX(), contour.getY(), 4f, dotPaint);

                            }

                            List<FirebaseVisionPoint> leftEyeContours = face.getContour(
                                    FirebaseVisionFaceContour.LEFT_EYE).getPoints();
                            for (int i = 0; i < leftEyeContours.size(); i++) {
                                FirebaseVisionPoint contour = leftEyeContours.get(i);
                                if (i != (leftEyeContours.size() - 1)){
                                    canvas.drawLine(contour.getX(), contour.getY(), leftEyeContours.get(i + 1).getX(),leftEyeContours.get(i + 1).getY(), linePaint);

                                }else {
                                    canvas.drawLine(contour.getX(), contour.getY(), leftEyeContours.get(0).getX(),
                                            leftEyeContours.get(0).getY(), linePaint);
                                }
                                canvas.drawCircle(contour.getX(), contour.getY(), 4f, dotPaint);


                            }

                            List<FirebaseVisionPoint> rightEyeContours = face.getContour(
                                    FirebaseVisionFaceContour.RIGHT_EYE).getPoints();
                            for (int i = 0; i < rightEyeContours.size(); i++) {
                                FirebaseVisionPoint contour = rightEyeContours.get(i);
                                if (i != (rightEyeContours.size() - 1)){
                                    canvas.drawLine(contour.getX(), contour.getY(), rightEyeContours.get(i + 1).getX(),rightEyeContours.get(i + 1).getY(), linePaint);

                                }else {
                                    canvas.drawLine(contour.getX(), contour.getY(), rightEyeContours.get(0).getX(),
                                            rightEyeContours.get(0).getY(), linePaint);
                                }
                                canvas.drawCircle(contour.getX(), contour.getY(), 4f, dotPaint);


                            }

                            List<FirebaseVisionPoint> upperLipTopContour = face.getContour(
                                    FirebaseVisionFaceContour.UPPER_LIP_TOP).getPoints();
                            for (int i = 0; i < upperLipTopContour.size(); i++) {
                                FirebaseVisionPoint contour = upperLipTopContour.get(i);
                                if (i != (upperLipTopContour.size() - 1)){
                                    canvas.drawLine(contour.getX(), contour.getY(),
                                            upperLipTopContour.get(i + 1).getX(),
                                            upperLipTopContour.get(i + 1).getY(), linePaint);
                                }
                                canvas.drawCircle(contour.getX(), contour.getY(), 4f, dotPaint);

                            }

                            List<FirebaseVisionPoint> upperLipBottomContour = face.getContour(
                                    FirebaseVisionFaceContour.UPPER_LIP_BOTTOM).getPoints();
                            for (int i = 0; i < upperLipBottomContour.size(); i++) {
                                FirebaseVisionPoint contour = upperLipBottomContour.get(i);
                                if (i != (upperLipBottomContour.size() - 1)){
                                    canvas.drawLine(contour.getX(), contour.getY(), upperLipBottomContour.get(i + 1).getX(),upperLipBottomContour.get(i + 1).getY(), linePaint);
                                }
                                canvas.drawCircle(contour.getX(), contour.getY(), 4f, dotPaint);

                            }
                            List<FirebaseVisionPoint> lowerLipTopContour = face.getContour(
                                    FirebaseVisionFaceContour.LOWER_LIP_TOP).getPoints();
                            for (int i = 0; i < lowerLipTopContour.size(); i++) {
                                FirebaseVisionPoint contour = lowerLipTopContour.get(i);
                                if (i != (lowerLipTopContour.size() - 1)){
                                    canvas.drawLine(contour.getX(), contour.getY(), lowerLipTopContour.get(i + 1).getX(),lowerLipTopContour.get(i + 1).getY(), linePaint);
                                }
                                canvas.drawCircle(contour.getX(), contour.getY(), 4f, dotPaint);

                            }
                            List<FirebaseVisionPoint> lowerLipBottomContour = face.getContour(
                                    FirebaseVisionFaceContour.LOWER_LIP_BOTTOM).getPoints();
                            for (int i = 0; i < lowerLipBottomContour.size(); i++) {
                                FirebaseVisionPoint contour = lowerLipBottomContour.get(i);
                                if (i != (lowerLipBottomContour.size() - 1)){
                                    canvas.drawLine(contour.getX(), contour.getY(), lowerLipBottomContour.get(i + 1).getX(),lowerLipBottomContour.get(i + 1).getY(), linePaint);
                                }
                                canvas.drawCircle(contour.getX(), contour.getY(), 4f, dotPaint);

                            }

                            List<FirebaseVisionPoint> noseBridgeContours = face.getContour(
                                    FirebaseVisionFaceContour.NOSE_BRIDGE).getPoints();
                            for (int i = 0; i < noseBridgeContours.size(); i++) {
                                FirebaseVisionPoint contour = noseBridgeContours.get(i);
                                if (i != (noseBridgeContours.size() - 1)) {
                                    canvas.drawLine(contour.getX(), contour.getY(), noseBridgeContours.get(i + 1).getX(),noseBridgeContours.get(i + 1).getY(), linePaint);
                                }
                                canvas.drawCircle(contour.getX(), contour.getY(), 4f, dotPaint);

                            }

                            List<FirebaseVisionPoint> noseBottomContours = face.getContour(
                                    FirebaseVisionFaceContour.NOSE_BOTTOM).getPoints();
                            for (int i = 0; i < noseBottomContours.size(); i++) {
                                FirebaseVisionPoint contour = noseBottomContours.get(i);
                                if (i != (noseBottomContours.size() - 1)) {
                                    canvas.drawLine(contour.getX(), contour.getY(), noseBottomContours.get(i + 1).getX(),noseBottomContours.get(i + 1).getY(), linePaint);
                                }
                                canvas.drawCircle(contour.getX(), contour.getY(), 4f, dotPaint);

                            }
                            if (cameraFacing == Facing.FRONT) {
                                //Flip image!
                                Matrix matrix = new Matrix();
                                matrix.preScale(-1f, 1f);
                                Bitmap flippedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                                        bitmap.getWidth(), bitmap.getHeight(),
                                        matrix, true);
                                imageView.setImageBitmap(flippedBitmap);
                            }else
                                imageView.setImageBitmap(bitmap);
                        }//end forloop*/


                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    imageView.setImageBitmap(null);

                }
            });

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void option_click(View view) {
        if (current_firebaseVisionFaces == null || current_bitmap == null) {
            Toast.makeText(this, "There was an error", Toast.LENGTH_SHORT).show();
            return;
        }
        Bitmap tempmap=current_bitmap.copy(Bitmap.Config.ARGB_8888, true);;
        Canvas canvas = new Canvas(tempmap);
        Paint landmarkPaint = new Paint();
        landmarkPaint.setColor(Color.RED);
        landmarkPaint.setStyle(Paint.Style.FILL);
        landmarkPaint.setStrokeWidth(11f);

        if (view.getId() == R.id.image_glass) {
            Paint drawglass = new Paint();
            Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.sunglasses);
            drawglass.setColor(Color.RED);
            //todo DRAW GLASS AND STICKERS
            //todo ^_^
            for (int i = 0; i < current_firebaseVisionFaces.size(); i++) {
                FirebaseVisionFace face = current_firebaseVisionFaces.get(i);
                float lx = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EYE).getPosition().getX();
                float ly = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EYE).getPosition().getY();
                float rx = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EYE).getPosition().getX();
                float ry = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EYE).getPosition().getY();
                float delta_x = lx - rx;
                float delta_y = ly - ry;
                float size =  abs(face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR).getPosition().getX() - face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EAR).getPosition().getX());
                double f = -1.0;
                double angel = atan2(delta_y, delta_x) * f;

                Matrix matrix = new Matrix();
                //matrix.postRotate(Math.abs(face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EYE).getPosition().getY()-face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_CHEEK).getPosition().getY()));
                matrix.postRotate((float) angel);
                Bitmap bb = Bitmap.createScaledBitmap(b, (int)size, (int)size, false);
                Bitmap rotatedBitmap = Bitmap.createBitmap(bb, 0, 0, bb.getWidth(), bb.getHeight(), matrix, true);
                // Toast.makeText(this, delta_y+" ☺  "+delta_z, Toast.LENGTH_LONG).show();
                // Toast.makeText(this, Math.abs(Math.abs(face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EYE).getPosition().getY())-Math.abs(face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EYE).getPosition().getY()))+"", Toast.LENGTH_SHORT).show();
                float leftear = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR).getPosition().getX();
                float base = face.getLandmark(FirebaseVisionFaceLandmark.NOSE_BASE).getPosition().getX();
                float t = abs(leftear) + (size / 2.0f);
                float temp = 0;
//            if(abs(leftear)-abs(lx)<100)
//                temp-=100;
                //if(abs(abs(t)-abs(base))>=10)
                float finish;
                temp += abs(abs(t) - abs(base));
                if (t < abs(base)) {
                     //       Toast.makeText(this, leftear+" "+t+" "+base+" "+temp, Toast.LENGTH_SHORT).show();
                    finish = leftear + temp-10;
                } else {
                    finish = leftear - temp;
                  //    Toast.makeText(this, lx+" b "+rx, Toast.LENGTH_SHORT).show();

                }
            /*Toast.makeText(this, "Leftear  "+leftear+
                            "\nleftear+temp  "+(leftear+temp)+
                            "\nleftear-temp  "+(leftear-temp)+
                            "\ntemp  "+temp+
                            "\nbase  "+base
                    , Toast.LENGTH_LONG).show();*/
                canvas.drawBitmap(rotatedBitmap, finish,
                        face.getBoundingBox().top, drawglass);
            }
        } else {


            for (int i = 0; i < current_firebaseVisionFaces.size(); i++) {
                FirebaseVisionFace face = current_firebaseVisionFaces.get(i);

                if (face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EYE) != null) {
                    FirebaseVisionFaceLandmark leftEye = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EYE);
                canvas.drawCircle(Objects.requireNonNull(leftEye).getPosition().getX(),
                        leftEye.getPosition().getY(),
                        8f,
                        landmarkPaint
                );
                }
                if (face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EYE) != null) {
                    FirebaseVisionFaceLandmark rightEye = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EYE);
                canvas.drawCircle(Objects.requireNonNull(rightEye).getPosition().getX(),
                        rightEye.getPosition().getY(),
                        8f,
                        landmarkPaint
                );
                }


            if (face.getLandmark(FirebaseVisionFaceLandmark.NOSE_BASE) != null) {
                FirebaseVisionFaceLandmark nose = face.getLandmark(FirebaseVisionFaceLandmark.NOSE_BASE);
                canvas.drawCircle(Objects.requireNonNull(nose).getPosition().getX(),
                        nose.getPosition().getY(),
                        8f,
                        landmarkPaint
                );
            }
            if (face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR) != null) {
                FirebaseVisionFaceLandmark leftEar = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR);
                canvas.drawCircle(Objects.requireNonNull(leftEar).getPosition().getX(),
                        leftEar.getPosition().getY(),
                        8f,
                        landmarkPaint

                );
            }

           if (face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EAR) != null) {
                FirebaseVisionFaceLandmark rightEar = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EAR);
                canvas.drawCircle(Objects.requireNonNull(rightEar).getPosition().getX(),
                        rightEar.getPosition().getY(),
                        8f,
                        landmarkPaint
                );
            }
            if (face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_LEFT) != null
                    && face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_BOTTOM) != null
                    && face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_RIGHT) != null) {
                FirebaseVisionFaceLandmark leftMouth = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_LEFT);
                FirebaseVisionFaceLandmark bottomMouth = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_BOTTOM);
                FirebaseVisionFaceLandmark rightMouth = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_RIGHT);
                canvas.drawLine(leftMouth.getPosition().getX(),
                        leftMouth.getPosition().getY(),
                        bottomMouth.getPosition().getX(),
                        bottomMouth.getPosition().getY(),
                        landmarkPaint);
                canvas.drawLine(bottomMouth.getPosition().getX(),
                        bottomMouth.getPosition().getY(),
                        rightMouth.getPosition().getX(),
                        rightMouth.getPosition().getY(), landmarkPaint);
            }

            }
        }
        imageView.setImageBitmap(tempmap);
    }
}
