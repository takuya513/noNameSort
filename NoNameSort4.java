package noNameSort;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import tools.MyArrayUtil;


public class NoNameSort4<E extends Comparable> {
	E[] array,ori;
	int threadsNum,arrayLength,pos = 0,first,mid;
	volatile boolean[] isSorted,ori2;
	boolean judge;
	ExecutorService executor;
	LinkedList<Callable<Object>> workers;
	int[] nextPos;//次のmiddle or right を覚えておく配列

	static final int UPUP = 1;
	static final int UPDOWN = 2;
	static final int DOWNUP = 3;
	static final int DOWNDOWN = 4;

	public NoNameSort4(){

	}

	public void sort(E[] array){
		this.array = array;
		this.ori = Arrays.copyOf(array, array.length);
		arrayLength = array.length;
		isSorted = new boolean[arrayLength];
		judge = true;
		nextPos = new int[arrayLength];

		Arrays.fill(isSorted,true);
		executor = Executors.newFixedThreadPool(4);
		workers = new LinkedList<Callable<Object>>();

		//第一段階、最初に4パターンに分かれている配列をマージしていき、すべて一パターンの配列にする
		first();
		//ここまでできたらテスト
		try {
			printBoolean();
			MyArrayUtil.print(array);
			ori2 = Arrays.copyOf(isSorted, isSorted.length);
			executor.invokeAll(workers);
			executor.shutdown();
			MyArrayUtil.print(array);
			printBoolean();
			System.out.println("before");
			MyArrayUtil.print(ori);
			//printBoolean(ori2);
		} catch (InterruptedException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

	}



	@SuppressWarnings("unchecked")
	public void first(){
		int num = 0;
		while(pos+1 < arrayLength){  //配列の最後まで
			first = pos;
			if(array[pos].compareTo(array[pos+1]) <= 0){
				while(array[pos].compareTo(array[pos+1]) <= 0){
					pos++;
					//ひたすら上り続けた時
					if(pos+1 >= arrayLength){
						markBoolean(num,first,pos);//oxをつける

						nextPos[first] = pos;//次のポジションの保存
						workers.add(Executors.callable(new MergeSortWorker(first,pos,num++)));
						return;
					}
				}
				mid = pos;
				pos++;

				//もし次の要素がなかったら
				if(pos == arrayLength-1){
					markBoolean(num,first,pos);//oxをつける
					nextPos[first] = pos;//次のポジションの保存
					insertSort(first,pos);
					workers.add(Executors.callable(new MergeSortWorker(first,pos,num++)));
					return;
				}

				//どっちに転ぶか？
				if(array[pos].compareTo(array[pos+1]) <= 0){  //パターン1
					while(array[pos].compareTo(array[pos+1]) <= 0){
						pos++;
						if(pos+1 >= arrayLength){
							break;
						}
					}
					markBooleanFirst(first,mid,pos);
					nextPos[first] = pos;//次のポジションの保存
					workers.add(Executors.callable(new MergeSortWorker(first,mid,pos,num++,UPUP)));
				}else{  //パターン２
					while(array[pos].compareTo(array[pos+1]) > 0){
						pos++;

						if(pos+1 >= arrayLength){
							break;
						}
					}
					markBooleanFirst(first,mid,pos);
					nextPos[first] = pos;//次のポジションの保存
					workers.add(Executors.callable(new MergeSortWorker(first,mid,pos,num++,UPDOWN)));
				}

			}else{
				while(array[pos].compareTo(array[pos+1]) > 0){
					pos++;
					if(pos+1 >= arrayLength){
						reverse(first,pos);  //すべてが逆順のときだったので逆にして終了
						nextPos[first] = pos;//次のポジションの保存
						workers.add(Executors.callable(new MergeSortWorker(first,pos,num++)));
						markBoolean(num,first,pos);	//oxをつける
						return;
					}
				}
				mid = pos;
				pos++;   //次に進める

				//もし次の要素がなかったら
				if(pos == arrayLength-1){  //余りの配列かつ最後に一つだけ残ったら
					markBoolean(num,first,pos);//oxをつける
					nextPos[first] = pos;//次のポジションの保存
					insertSort(first,pos);
					workers.add(Executors.callable(new MergeSortWorker(first,pos,num++)));
					return;
				}

				//どっちに転ぶか？
				if(array[pos].compareTo(array[pos+1]) <= 0){  //パターン3
					while(array[pos].compareTo(array[pos+1]) <= 0){
						System.out.println("pattern3");
						pos++;
						if(pos+1 >= arrayLength){
							System.out.println("pattern3 Last");
							break;
						}
					}
					nextPos[first] = pos;//次のポジションの保存
					markBooleanFirst(first,mid,pos);
					workers.add(Executors.callable(new MergeSortWorker(first,mid,pos,num++,DOWNUP)));
				}else{  //パターン4
					while(array[pos].compareTo(array[pos+1]) > 0){
						System.out.println("pattern4");
						pos++;
						if(pos+1 >= arrayLength){
							System.out.println("pattern4 Last");
							break;
						}
					}
					nextPos[first] = pos;//次のポジションの保存
					markBooleanFirst(first,mid,pos);
					workers.add(Executors.callable(new MergeSortWorker(first,mid,pos,num++,DOWNDOWN)));

				}
			}


			pos++;
			if(pos == arrayLength-1){
				nextPos[pos] = pos;//次のポジションの保存
				isSorted[pos] = judge;
				workers.add(Executors.callable(new MergeSortWorker(pos,pos,num++)));
			}
		}


	}


	public void insertSort(int left,int right){
		int i,j;
		E temp;
		for(i = left + 1; i <=right;i++) {
			temp = array[i];
			j = i;
			while(j > left && temp.compareTo(array[j-1])< 0){
				array[j] = array[j-1];
				j--;
			}
			array[j] = temp;
		}
	}




	class MergeSortWorker implements Runnable{
		int num,nextRight,id;
		int left,right,mid,pattern;
		boolean isNext = true;
		boolean isRest = false;
		Object[] buff;



		public MergeSortWorker(int left,int mid,int right,int num,int pattern){
			this.num = num;
			this.pattern = pattern;
			this.left = left;
			this.mid = mid;
			this.right = right;
			id = num;

		}

		public MergeSortWorker(int left,int right,int num){
			this.num = num;
			this.left = left;
			this.right = right;
			isRest = true;
			id = num;
		}


		public void run(){


			if(!isRest){   //余りの配列じゃなければ,はじめのソートを行う
				switch(pattern){
				case UPUP:
					merge(left,mid,right);
					break;
				case UPDOWN:
					mergeUpDown(left,mid,right);
					break;
				case DOWNUP:
					mergeDownUp(left,mid,right);
					break;
				case DOWNDOWN:
					mergeDownDown(left,mid,right);
					break;
				default :
					break;

				}
			
			}

			markBoolean(num,left,right);
			printAll("FIRST MERGE");
			
			do{
				//偶数か奇数で分ける

				//偶数であれば
				if(num % 2 == 0){
					printAll();
					//余りであれば　　　このrightがarrayLength - 1で判断する
					if(right == arrayLength - 1){
						//左側を監視し続ける
						while(isSorted[left - 1] != isSorted[left]){
							
							System.out.println("id["+id+"] is waitting");
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
								// TODO 自動生成された catch ブロック
								e.printStackTrace();
							}
						}
						//左がoになったら
						//このbooleanをxにする
						markBoolean(1,left,right);
					}else{//余りでなければ
						//右を監視し続ける
						while(isSorted[right] == isSorted[right + 1]){
							System.out.println("id["+id+"] is waitting");
							printAll();
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
								// TODO 自動生成された catch ブロック
								e.printStackTrace();
							}
						}
						//右がxになったら
						//int[]からkjmmk,
						//mid = int[left]  right = int[mid] に設定しmergeする
						mid = right;
						right = nextPos[mid+1];
						merge(left,mid,right);
						printAll("NEXT MERGE");
						if(left == 0 && right == arrayLength - 1){
							System.out.println("Last Merge");
							return; //最後のソートであったら終了
						}
						nextPos[left] = right;	//merge終わったらint[left] = rightに設定
						System.out.println("NUM    "+num+"  ---->"+num/2);
						num = num / 2;	//num = num / 2;
						
						markBoolean(num,left,right);	//booleanを更新
					}
				}else {//奇数であれば
					printAll();
					System.out.println("id "+id+"  END");
					return;//終了
				}

				System.out.println("id "+id+"  WIHLE CHECK");
				printAll();
			}while(true);



		}


		private synchronized void printAll() {
			System.out.println(+num+"    left : "+left+"  right : "+right+" id : "+id+"num : "+num);
			MyArrayUtil.print(array);
			MyArrayUtil.print(nextPos);
			printBoolean();
			System.out.println();
		}


		private synchronized void printAll(String st) {
			System.out.println(num+ "      "+st);
			System.out.println(+num+"    left : "+left+"  right : "+right+" id : "+id+"num : "+num);
			MyArrayUtil.print(array);
			MyArrayUtil.print(nextPos);
			printBoolean();
			System.out.println();
		}

	}

	private synchronized void markBoolean(int num,int left,int right) {
		if(num%2 == 0){	//numが奇数だったらすべてにtrueを代入
			for(int i = left;i <= right;i++){
				isSorted[i] = true;
			}
		}else{
			for(int i = left;i <= right;i++){
				isSorted[i] = false;
			}
		}
	}

	private synchronized void markBooleanFirst(int left,int mid,int right) {
			for(int i = left;i <= mid;i++){
				isSorted[i] = true;
			}
			for(int i = mid+1;i <= right;i++){
				isSorted[i] = false;
			}
		}


	public void merge(int left,int mid,int right){
	Object[] buff = new Object[right - left + 1];
		int i = left,j = mid + 1,k = 0;

		while(i <= mid && j <= right) {
			if(array[i].compareTo(array[j]) < 0)
				buff[k++] = array[i++];
			else
				buff[k++] = array[j++];
		}

		while(i <= mid)
			buff[k++] = array[i++];
		while(j <= right)
			buff[k++] = array[j++];
		for(i = left;i <= right; i++)
			array[i] = (E) buff[i - left];
	}

	private synchronized void printBoolean() {
		for(int i = 0;i < arrayLength;i++){
			if(isSorted[i] == true){
				System.out.print(" o");
			}else{
				System.out.print(" x");
			}
		}
		System.out.println();
	}



	private void reverse(int left, int right) {
		int range = right - left + 1;
		Object[] tmp = new Object[range];

		for(int i = 0;i < range;i++)
			tmp[i] = array[right-i];

		for(int i = 0;i < range;i++){
			array[i + left] = (E) tmp[i];
		}

	}


	public void mergeUpDown(int left,int mid,int right){
		Object[] buff = new Object[right - left + 1];
		int i = left,j = right,k = 0;

		while(i <= mid && j > mid) {
			if(array[i].compareTo(array[j]) < 0)
				buff[k++] = array[i++];
			else
				buff[k++] = array[j--];
		}

		while(i <= mid)
			buff[k++] = array[i++];
		while(j > mid)
			buff[k++] = array[j--];
		for(i = left;i <= right; i++)
			array[i] = (E) buff[i - left];
	}

	public void mergeDownUp(int left,int mid,int right){
		Object[] buff = new Object[right - left + 1];
		int i = mid,j = mid+1,k = 0;

		while(i >= left && j <= right) {
			if(array[i].compareTo(array[j]) < 0)
				buff[k++] = array[i--];
			else
				buff[k++] = array[j++];
		}

		while(i >= left)
			buff[k++] = array[i--];
		while(j <= right)
			buff[k++] = array[j++];
		for(i = left;i <= right; i++)
			array[i] = (E) buff[i - left];
	}

	public void mergeDownDown(int left,int mid,int right){
		Object[] buff = new Object[right - left + 1];
		int i = mid,j = right,k = 0;

		while(i >= left && j > mid) {
			if(array[i].compareTo(array[j]) < 0)
				buff[k++] = array[i--];
			else
				buff[k++] = array[j--];
		}

		while(i >= left)
			buff[k++] = array[i--];
		while(j > mid)
			buff[k++] = array[j--];
		for(i = left;i <= right; i++)
			array[i] = (E) buff[i - left];
	}

}
