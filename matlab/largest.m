function x = largest()
    array = [2 4 3 5; 
             1 2 3 4];
    
    part = [2 4 3 5];
    b = [1:4;array(1,:)];
    disp(b);
    b = b(:);
    disp(b)
    x = sortrows(array);
    
end