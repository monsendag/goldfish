% // ==================================
% // test program
% // ==================================
clear

%kiwi = Kiwi('../datasets/movielens-synthesized/ratings-synthesized.csv');
%kiwi.recommend(1, 2)


X = sptenrand([3 3 2],10);
T = tucker_als(X, 2);

F = full(T);
% // ==================================
% // test program
% // ==================================
clear

kiwi = Kiwi('../datasets/movielens-synthesized/ratings-synthesized-200k.csv',[1 1],[300 500 2]);
fprintf('Index: %d,%d,%d has rating %f, should be %d \n',1, 1193, 0, kiwi.get_rating(1,1193,1), 5);
fprintf('Index: %d,%d,%d has rating %f, should be %d \n',1, 661, 0, kiwi.get_rating(1,661,1), 3);
fprintf('Index: %d,%d,%d has rating %f, should be %d \n',1, 594, 1, kiwi.get_rating(1,594,2), 1.6);
fprintf('Index: %d,%d,%d has rating %f, should be %d \n',1, 1193, 1, kiwi.get_rating(1,1193,2), 2.2);
%kiwi.recommend(1, 2)

r = rmse(kiwi.sparse_tensor, kiwi.dense_tensor);

tic;
fprintf('RMSE is: %f',r);
toc;