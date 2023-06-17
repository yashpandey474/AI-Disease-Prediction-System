# AI-Disease-Prediction-System
AI-Disease Prediction System Project

This report focuses on the model of predictive diagnosis involving
data munging, feature selection, model training and accuracy
calculation. We try to introduce and validate methods that
improve certain aspects of this process.

# Assignment Problem Statement & Details
# Problem Statement [3-PART]:
1. The data munging process introduces significant bias depending on
majority of dataset by replacing with mean.
2. The feature selection process suffers from lack of adaptability by
keeping a constant p-value significance limit of 0.05 without variance
depending on the disease.
3. The datasets for diagnosis prediction must be general & not bias,
with enough data for each class of features.

For data munging, we propose improvements
through using the method of k-nearest-neighbors and extensively
compare the results with data munging done by replacing with
mean.

For the feature selection, we use the simulated annealing
algorithm to set up an environment of varying p-value within a
given upper bound and lower bound, defined initial temperature
and cooling rate. Evaluation of the model has been done by
testing on the training set rather than dividing the sets, although
results from cross-validation have been provided at numerous
places throughout the report. This has been supported by the
fact that the 90/10 Train-test method gave stale accuracies for
most p-values although they had different attribute sets and
considerable differences when predicting on the training set.
For data augmentation, to improve the diversity of the dataset -
we have proposed a few data augmentation techniques such as
adding noise, scaling etc.
