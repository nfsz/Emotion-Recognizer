//
// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license.
//
// Project Oxford: http://ProjectOxford.ai
//
// ProjectOxford SDK Github:
// https://github.com/Microsoft/ProjectOxfordSDK
//
// Copyright (c) Microsoft Corporation
// All rights reserved.
//
// MIT License:
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
//
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED ""AS IS"", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//
package com.microsoft.projectoxford.emotionsample;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.microsoft.projectoxford.emotion.EmotionServiceClient;
import com.microsoft.projectoxford.emotion.EmotionServiceRestClient;
import com.microsoft.projectoxford.emotion.contract.FaceRectangle;
import com.microsoft.projectoxford.emotion.contract.RecognizeResult;
import com.microsoft.projectoxford.emotion.rest.EmotionServiceException;
import com.microsoft.projectoxford.emotionsample.helper.ImageHelper;

import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.Face;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class RecognizeActivity extends ActionBarActivity {
    private MediaPlayer mp = null;

    Context context;

        // Flag to indicate which task is to be performed.
        private static final int REQUEST_SELECT_IMAGE = 0;

        // The button to select an image
        private Button mButtonSelectImage;

        // The URI of the image selected to detect.
        private Uri mImageUri;

        // The image selected to detect.
        private Bitmap mBitmap;

        // The edit to show status and result.
        private EditText mEditText;

        private EmotionServiceClient client;

        private Button bPlay, bPause;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_recognize);

            if (client == null) {
                client = new EmotionServiceRestClient(getString(R.string.subscription_key));
            }

            mButtonSelectImage = (Button) findViewById(R.id.buttonSelectImage);
            mEditText = (EditText) findViewById(R.id.editTextResult);

            context = this;

            bPause = (Button) findViewById(R.id.bPause);
            bPlay = (Button) findViewById(R.id.bPlay);
            bPause.setVisibility(View.GONE);
            bPlay.setVisibility(View.GONE);
            mEditText.setVisibility(View.GONE);
            bPause.setEnabled(false);
            bPlay.setEnabled(false);


            bPlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(getApplicationContext(), "Playing sound", Toast.LENGTH_SHORT).show();
                    mp.start();

                    bPause.setEnabled(true);
                    bPlay.setEnabled(false);
                }
            });

            bPause.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(getApplicationContext(), "Pausing sound",Toast.LENGTH_SHORT).show();
                    mp.pause();
                    bPause.setEnabled(false);
                    bPlay.setEnabled(true);
                }
            });


        }

        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            // Inflate the menu; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.menu_recognize, menu);
            return true;
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            // Handle action bar item clicks here. The action bar will
            // automatically handle clicks on the Home/Up button, so long
            // as you specify a parent activity in AndroidManifest.xml.
            int id = item.getItemId();

            //noinspection SimplifiableIfStatement
            if (id == R.id.action_settings) {
                return true;
            }

            return super.onOptionsItemSelected(item);
        }

        public void doRecognize() {
            mButtonSelectImage.setEnabled(false);

            // Do emotion detection using auto-detected faces.
            try {
                new doRequest(false).execute();
            } catch (Exception e) {
                mEditText.append("Error encountered. Exception is: " + e.toString());
            }

            String faceSubscriptionKey = getString(R.string.faceSubscription_key);
            if (faceSubscriptionKey.equalsIgnoreCase("Please_add_the_face_subscription_key_here")) {
                mEditText.append("\n\nThere is no face subscription key in res/values/strings.xml. Skip the sample for detecting emotions using face rectangles\n");
            } else {
                // Do emotion detection using face rectangles provided by Face API.
                try {
                    new doRequest(true).execute();
                } catch (Exception e) {
                    mEditText.append("Error encountered. Exception is: " + e.toString());
                }
            }
        }

    @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first

        if (mp != null)
            mp.release();

        bPause.setVisibility(View.GONE);
        bPlay.setVisibility(View.GONE);
        bPause.setEnabled(false);
        bPlay.setEnabled(false);

    }

        // Called when the "Select Image" button is clicked.
        public void selectImage(View view) {
            mEditText.setText("");
            Intent intent;
            intent = new Intent(RecognizeActivity.this, com.microsoft.projectoxford.emotionsample.helper.SelectImageActivity.class);
            startActivityForResult(intent, REQUEST_SELECT_IMAGE);
        }

        // Called when image selection is done.
        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            Log.d("RecognizeActivity", "onActivityResult");
            switch (requestCode) {
                case REQUEST_SELECT_IMAGE:
                    if (resultCode == RESULT_OK) {
                        // If image is selected successfully, set the image URI and bitmap.
                        mImageUri = data.getData();

                        mBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(
                                mImageUri, getContentResolver());
                        if (mBitmap != null) {
                            // Show the image on screen.
                            ImageView imageView = (ImageView) findViewById(R.id.selectedImage);
                            imageView.setImageBitmap(mBitmap);

                            // Add detection log.
                            Log.d("RecognizeActivity", "Image: " + mImageUri + " resized to " + mBitmap.getWidth()
                                    + "x" + mBitmap.getHeight());

                            doRecognize();
                        }
                    }
                    break;
                default:
                    break;
            }
        }


        private List<RecognizeResult> processWithAutoFaceDetection() throws EmotionServiceException, IOException {
            Log.d("emotion", "Start emotion detection with auto-face detection");

            Gson gson = new Gson();

            // Put the image into an input stream for detection.
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

            long startTime = System.currentTimeMillis();
            // -----------------------------------------------------------------------
            // KEY SAMPLE CODE STARTS HERE
            // -----------------------------------------------------------------------

            List<RecognizeResult> result = null;
            //
            // Detect emotion by auto-detecting faces in the image.
            //
            result = this.client.recognizeImage(inputStream);

            String json = gson.toJson(result);
            Log.d("result", json);

            Log.d("emotion", String.format("Detection done. Elapsed time: %d ms", (System.currentTimeMillis() - startTime)));
            // -----------------------------------------------------------------------
            // KEY SAMPLE CODE ENDS HERE
            // -----------------------------------------------------------------------
            return result;
        }

        private List<RecognizeResult> processWithFaceRectangles() throws EmotionServiceException, com.microsoft.projectoxford.face.rest.ClientException, IOException {
            Log.d("emotion", "Do emotion detection with known face rectangles");
            Gson gson = new Gson();

            // Put the image into an input stream for detection.
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

            long timeMark = System.currentTimeMillis();
            Log.d("emotion", "Start face detection using Face API");
            FaceRectangle[] faceRectangles = null;
            String faceSubscriptionKey = getString(R.string.faceSubscription_key);
            FaceServiceRestClient faceClient = new FaceServiceRestClient(faceSubscriptionKey);
            Face faces[] = faceClient.detect(inputStream, false, false, null);
            Log.d("emotion", String.format("Face detection is done. Elapsed time: %d ms", (System.currentTimeMillis() - timeMark)));

            if (faces != null) {
                faceRectangles = new FaceRectangle[faces.length];

                for (int i = 0; i < faceRectangles.length; i++) {
                    // Face API and Emotion API have different FaceRectangle definition. Do the conversion.
                    com.microsoft.projectoxford.face.contract.FaceRectangle rect = faces[i].faceRectangle;
                    faceRectangles[i] = new com.microsoft.projectoxford.emotion.contract.FaceRectangle(rect.left, rect.top, rect.width, rect.height);
                }
            }

            List<RecognizeResult> result = null;
            if (faceRectangles != null) {
                inputStream.reset();

                timeMark = System.currentTimeMillis();
                Log.d("emotion", "Start emotion detection using Emotion API");
                // -----------------------------------------------------------------------
                // KEY SAMPLE CODE STARTS HERE
                // -----------------------------------------------------------------------
                result = this.client.recognizeImage(inputStream, faceRectangles);

                String json = gson.toJson(result);
                Log.d("result", json);
                // -----------------------------------------------------------------------
                // KEY SAMPLE CODE ENDS HERE
                // -----------------------------------------------------------------------
                Log.d("emotion", String.format("Emotion detection is done. Elapsed time: %d ms", (System.currentTimeMillis() - timeMark)));
            }
            return result;
        }

        private class doRequest extends AsyncTask<String, String, List<RecognizeResult>> {
            // Store error message
            private Exception e = null;
            private boolean useFaceRectangles = false;

            public doRequest(boolean useFaceRectangles) {
                this.useFaceRectangles = useFaceRectangles;
            }

            @Override
            protected List<RecognizeResult> doInBackground(String... args) {
                if (this.useFaceRectangles == false) {
                    try {
                        return processWithAutoFaceDetection();
                    } catch (Exception e) {
                        this.e = e;    // Store error
                    }
                } else {
                    try {
                        return processWithFaceRectangles();
                    } catch (Exception e) {
                        this.e = e;    // Store error
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(List<RecognizeResult> result) {
                super.onPostExecute(result);
                // Display based on error existence

                if (this.useFaceRectangles == false) {
                    mEditText.append("\n\nRecognizing emotions with auto-detected face rectangles...\n");
                } else {
                    mEditText.append("\n\nRecognizing emotions with existing face rectangles from Face API...\n");
                }
                if (e != null) {
                    mEditText.setText("Error: " + e.getMessage());
                    this.e = null;
                } else {
                    if (result.size() == 0) {
                        mEditText.append("No emotion detected :(");
                    } else {
                        Integer count = 0;
                        /*Canvas faceCanvas = new Canvas(mBitmap);
                        faceCanvas.drawBitmap(mBitmap, 0, 0, null);*/
                        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(5);
                        paint.setColor(Color.RED);
                        double x = 0;
                        String largest = "";
                        for(RecognizeResult r : result){
                            if(r.scores.anger>x){x=r.scores.anger; largest = "anger";}
                            if(r.scores.contempt>x){x=r.scores.contempt;largest = "contempt";}
                            if(r.scores.disgust>x){x=r.scores.disgust;largest = "disgust";}
                            if(r.scores.fear>x){x=r.scores.fear;largest = "fear";}
                            if(r.scores.happiness>x){x=r.scores.happiness;largest = "happiness";}
                            if(r.scores.neutral>x){x=r.scores.neutral;largest = "neutral";}
                            if(r.scores.sadness>x){x=r.scores.sadness;largest = "sadness";}
                            if(r.scores.surprise>x){x=r.scores.surprise;largest = "surprise";}
                        }
                        if(largest.equals("anger") ) {
                            //angry.start();
                            mp = MediaPlayer.create(context,R.raw.anger);
                        }
                        else if (largest.equals("disgust")){
                            mp = MediaPlayer.create(context,R.raw.disgust);
                        }
                        else if (largest.equals("neutral")){
                            mp = MediaPlayer.create(context,R.raw.neutral);
                        }
                        else if(largest.equals("sadness") || largest.equals("contempt")){
                            mp = MediaPlayer.create(context,R.raw.sad);
                        }
                        else if(largest.equals("fear")){
                            mp = MediaPlayer.create(context,R.raw.scary);
                        }
                        else if (largest.equals("surprise")){
                            mp = MediaPlayer.create(context,R.raw.surprise);
                        }
                        else if(largest.equals("happiness")){
                            mp = MediaPlayer.create(context,R.raw.happy);
                        }
                        else{}//do nothing in this situation

                        mp.start();
                        bPause.setVisibility(View.VISIBLE);
                        bPlay.setVisibility(View.VISIBLE);
                        bPause.setEnabled(true);
                        bPlay.setEnabled(false);

                        //mEditText.append(String.format("\t Score: %1$.5f\n", x));

                        Toast.makeText(getApplicationContext(), "Hmm, you seem to be feeling " + largest + ".", Toast.LENGTH_LONG).show();


                        //mEditText.append(largest);


                        //for (RecognizeResult r : result) {
                            //mEditText.append(String.format("\nFace #%1$d \n", count));
                            //mEditText.append(String.format("\t anger: %1$.5f\n", r.scores.anger));
                            //mEditText.append(String.format("\t contempt: %1$.5f\n", r.scores.contempt));
                            //mEditText.append(String.format("\t disgust: %1$.5f\n", r.scores.disgust));
                            //mEditText.append(String.format("\t fear: %1mEditText.append(String.format("\t anger: %1$.5f\n", r.scores.anger));$.5f\n", r.scores.fear));
                            //mEditText.append(String.format("\t happiness: %1$.5f\n", r.scores.happiness));
                            //mEditText.append(String.format("\t neutral: %1$.5f\n", r.scores.neutral));
                            //mEditText.append(String.format("\t sadness: %1$.5f\n", r.scores.sadness));
                            //mEditText.append(String.format("\t surprise: %1$.5f\n", r.scores.surprise));
                            //mEditText.append(String.format("\t face rectangle: %d, %d, %d, %d", r.faceRectangle.left, r.faceRectangle.top, r.faceRectangle.width, r.faceRectangle.height));
                            /*faceCanvas.drawRect(r.faceRectangle.left,
                                    r.faceRectangle.top,
                                    r.faceRectangle.left + r.faceRectangle.width,
                                    r.faceRectangle.top + r.faceRectangle.height,
                                    paint);*/
                          //  count++;
                        //}
                        ImageView imageView = (ImageView) findViewById(R.id.selectedImage);
                        imageView.setImageDrawable(new BitmapDrawable(getResources(), mBitmap));
                    }
                    mEditText.setSelection(0);
                }

                mButtonSelectImage.setEnabled(true);
            }
        }
}
