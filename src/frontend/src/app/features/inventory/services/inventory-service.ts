import { Service, signal, computed } from '@angular/core';
import { Product } from '../models/product';
import { httpResource } from '@angular/common/http';

@Service()
export class InventoryService {

  private readonly API_URL="http://127.0.0.1:8888/api/products"

  // create resource to get products list
  readonly productResource =httpResource<Product[]>(()=>this.API_URL);

  // add product method
  addProduct(product:Product){
    console.log("Adding product to backend.");
    //TODO add http post
  }

  constructor(){
    console.log('InventoryService initialized using @Service decorator');
  }
}
