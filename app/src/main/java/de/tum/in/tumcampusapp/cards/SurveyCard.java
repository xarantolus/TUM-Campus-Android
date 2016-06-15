package de.tum.in.tumcampusapp.cards;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Date;

import de.tum.in.tumcampusapp.R;
import de.tum.in.tumcampusapp.auxiliary.Utils;
import de.tum.in.tumcampusapp.models.Question;
import de.tum.in.tumcampusapp.models.managers.CardManager;
import de.tum.in.tumcampusapp.models.managers.SurveyManager;

public class SurveyCard extends Card

{
    private final ArrayList<Question> questions = new ArrayList<>(); // gets filled with the revelant openQuestions for the card
    private final SurveyManager manager = new SurveyManager(mContext);
    private TextView mQuestion;
    private Button bYes;
    private Button bNo;
    private Button bSkip;
    private ImageButton bFlagged;
    private DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"); // For converting Jade DateTime into String & vic versa (see show and discard functions)
    // Answer flags relevant for updating the answered questions in the db

    private static int answerYes = 1;
    private static int answerNo = 2;
    private static int answerFlag = -1;
    private static int answerSkip = 3;

    public SurveyCard(Context context) {
        super(context, "card_survey");
    }

    public static Card.CardViewHolder inflateViewHolder(final ViewGroup parent) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_survey, parent, false);
        return new Card.CardViewHolder(view);
    }

    @Override
    public int getTyp() {
        return CardManager.CARD_SURVEY;
    }

    /**
     * Handls the changing content of the survey card
     *
     * @param viewHolder The Card specific view holder
     */
    @Override
    public void updateViewHolder(RecyclerView.ViewHolder viewHolder) {
        super.updateViewHolder(viewHolder);
        mCard = viewHolder.itemView;
        mLinearLayout = (LinearLayout) mCard.findViewById(R.id.card_view);
        mTitleView = (TextView) mCard.findViewById(R.id.card_title);
        mQuestion = (TextView) mCard.findViewById(R.id.questionText);
        bYes = (Button) mCard.findViewById(R.id.yesAnswerCard);
        bNo = (Button) mCard.findViewById(R.id.noAnswerCard);
        bSkip = (Button) mCard.findViewById(R.id.ignoreAnswerCard);
        bFlagged = (ImageButton) mCard.findViewById(R.id.flagButton);

        showFirstQuestion();

    }

    /**
     * 1. Updates the answered question in the db
     * 2. Changes the content of the survey card depending on the questions ArrayList
     */
    private void showFirstQuestion() {
        mTitleView.setText(R.string.research_quiz);

        if (!questions.isEmpty()) {
            final Question ques = questions.get(0);
            mQuestion.setText(ques.getText()); // Sets the text of the question that should be shown first

            // Listens on the yes button in the card
            bYes.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Question updatedElement = questions.remove(0);
                    manager.updateQuestion(updatedElement, answerYes); // update the answerID in the local db.
                    showNextQuestions(); // handel showing next question(s)
                }
            });
            bNo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Question updatedElement = questions.remove(0);
                    manager.updateQuestion(updatedElement, answerNo); // update the answerID in the local db.
                    showNextQuestions(); // handel showing next question(s)
                }
            });
            bSkip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Question updatedElement = questions.remove(0);
                    manager.updateQuestion(updatedElement, answerSkip); // update the answerID in the local db.
                    showNextQuestions(); // handel showing next question(s)
                }
            });
            bFlagged.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Question updatedElement = questions.remove(0);
                    manager.updateQuestion(updatedElement, answerFlag); // update the answerID in the local db.
                    showNextQuestions(); // handel showing next question(s)
                }
            });
        }
    }

    /**
     * Help function which calls showfirstquestion recursively
     * depending on the size of the question Array list
     */
    private void showNextQuestions() {
        // if the question arraylist is not empty, show the first question (the answered question before got removed from the list)
        if (questions.size() >= 1) {
            showFirstQuestion();
        } else { // show there are no questions avaliable anymore
            mQuestion.setText(R.string.no_questions_available);
            bYes.setVisibility(View.GONE);
            bNo.setVisibility(View.GONE);
            bSkip.setVisibility(View.GONE);
            bFlagged.setVisibility(View.GONE);
        }
    }

    /**
     * Handles discarding the survey card. Grace period of 24 hours
     * Card should be shown again depending on the next function
     *
     * @param editor Editor to be used for saving values
     */
    @Override
    public void discard(SharedPreferences.Editor editor) {
        DateTime discardedTill = DateTime.now().plusMinutes(1440); // in 24 hours
        String discardTimeString = discardedTill.toString(fmt);
        editor.putString("survey_card_discarded_till", discardTimeString);
    }

    /**
     * Shows the card if there are releveant unansweredQuestions (not expired)
     * AND the discard grace period (if there is any) is finished
     *
     * @param p
     * @return
     */
    @Override
    public boolean shouldShow(SharedPreferences p) {
        String currentDate = Utils.getDateTimeString(new Date());
        DateTime discardedTill = fmt.parseDateTime(p.getString("survey_card_discarded_till", DateTime.now().toString(fmt)));
        return (discardedTill.isBeforeNow() && (manager.getUnansweredQuestionsSince(currentDate).getCount() >= 1));
    }


    /**
     * Sets the openquestions (feteched from the server) in the  card
     *
     * @param cur: comprises the fetched openQuestions from the server
     */
    public void seQuestions(Cursor cur) {
        do {
            Question item = new Question(cur.getString(0), cur.getString(1));
            questions.add(item);
        } while (cur.moveToNext());
        cur.close();
    }
}
